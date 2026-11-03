# Servicio: el backend en ECS Fargate detrás de un balanceador (ALB).
# Sin estado en memoria, así que se puede escalar a varias copias.

variable "nombre" { type = string }
variable "region" { type = string }
variable "vpc_id" { type = string }
variable "subredes_publicas" { type = list(string) }
variable "copias" {
  type    = number
  default = 1
}
variable "cpu" {
  type    = number
  default = 512
}
variable "memoria" {
  type    = number
  default = 1024
}
variable "etiqueta_imagen" {
  type    = string
  default = "latest"
}
variable "variables_entorno" {
  type    = map(string)
  default = {}
}
variable "secretos" {
  description = "Nombre de la variable de entorno y ARN del secreto en Secrets Manager"
  type        = map(string)
  default     = {}
}
variable "arns_buckets" { type = list(string) }
variable "certificado_arn" {
  description = "Certificado de ACM para HTTPS. Vacío mientras no exista el dominio."
  type        = string
  default     = ""
}

resource "aws_ecr_repository" "backend" {
  name                 = "${var.nombre}-backend"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${var.nombre}-backend"
  retention_in_days = 30
}

resource "aws_security_group" "alb" {
  name   = "${var.nombre}-alb"
  vpc_id = var.vpc_id
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Las tareas están en subred pública (para no pagar NAT en pruebas), pero solo reciben tráfico del ALB.
resource "aws_security_group" "backend" {
  name   = "${var.nombre}-backend"
  vpc_id = var.vpc_id
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_lb" "principal" {
  name               = "${var.nombre}-alb"
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.subredes_publicas
}

resource "aws_lb_target_group" "backend" {
  name        = "${var.nombre}-backend"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"
  health_check {
    path    = "/actuator/health"
    matcher = "200"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.principal.arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_lb_listener" "https" {
  count             = var.certificado_arn == "" ? 0 : 1
  load_balancer_arn = aws_lb.principal.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificado_arn
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

data "aws_iam_policy_document" "asumir_ecs" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# Rol de ejecución: lo usa ECS para bajar la imagen, escribir registros y leer los secretos.
resource "aws_iam_role" "ejecucion" {
  name               = "${var.nombre}-ejecucion"
  assume_role_policy = data.aws_iam_policy_document.asumir_ecs.json
}

resource "aws_iam_role_policy_attachment" "ejecucion" {
  role       = aws_iam_role.ejecucion.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "leer_secretos" {
  count = length(var.secretos) > 0 ? 1 : 0
  name  = "leer-secretos"
  role  = aws_iam_role.ejecucion.id
  policy = jsonencode({
    Version = "2012-10-17"
    # ECS acepta "arn:...:secret:nombre:username::" para leer una llave del JSON, pero IAM necesita
    # el ARN del secreto sin ese sufijo. Se recortan las 3 últimas partes y se quitan repetidos.
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = distinct([for arn in values(var.secretos) : join(":", slice(split(":", arn), 0, 7))])
    }]
  })
}

# Rol de la tarea: lo que el código del backend puede hacer en AWS. Solo sus buckets.
resource "aws_iam_role" "tarea" {
  name               = "${var.nombre}-tarea"
  assume_role_policy = data.aws_iam_policy_document.asumir_ecs.json
}

resource "aws_iam_role_policy" "buckets" {
  name = "buckets-propios"
  role = aws_iam_role.tarea.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
      Resource = [for arn in var.arns_buckets : "${arn}/*"]
    }]
  })
}

resource "aws_ecs_cluster" "principal" {
  name = var.nombre
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${var.nombre}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.cpu
  memory                   = var.memoria
  execution_role_arn       = aws_iam_role.ejecucion.arn
  task_role_arn            = aws_iam_role.tarea.arn
  container_definitions = jsonencode([{
    name         = "backend"
    image        = "${aws_ecr_repository.backend.repository_url}:${var.etiqueta_imagen}"
    essential    = true
    portMappings = [{ containerPort = 8080 }]
    environment  = [for clave, valor in var.variables_entorno : { name = clave, value = valor }]
    secrets      = [for clave, arn in var.secretos : { name = clave, valueFrom = arn }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.backend.name
        awslogs-region        = var.region
        awslogs-stream-prefix = "backend"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${var.nombre}-backend"
  cluster         = aws_ecs_cluster.principal.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.copias
  launch_type     = "FARGATE"
  network_configuration {
    subnets          = var.subredes_publicas
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }
}

output "grupo_backend_id" { value = aws_security_group.backend.id }
output "url_balanceador" { value = aws_lb.principal.dns_name }
output "repositorio_imagen" { value = aws_ecr_repository.backend.repository_url }
