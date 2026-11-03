# Datos: PostgreSQL en RDS y Redis en ElastiCache, ambos en subredes privadas
# y solo accesibles desde el grupo de seguridad del backend.

variable "nombre" { type = string }
variable "vpc_id" { type = string }
variable "subredes_privadas" { type = list(string) }
variable "grupo_backend_id" { type = string }
variable "clase_rds" {
  type    = string
  default = "db.t4g.micro"
}
variable "nodo_redis" {
  type    = string
  default = "cache.t4g.micro"
}
variable "proteger_borrado" {
  type    = bool
  default = false
}

resource "aws_security_group" "postgres" {
  name   = "${var.nombre}-postgres"
  vpc_id = var.vpc_id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.grupo_backend_id]
  }
}

resource "aws_security_group" "redis" {
  name   = "${var.nombre}-redis"
  vpc_id = var.vpc_id
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.grupo_backend_id]
  }
}

resource "aws_db_subnet_group" "postgres" {
  name       = "${var.nombre}-postgres"
  subnet_ids = var.subredes_privadas
}

resource "aws_db_instance" "postgres" {
  identifier        = "${var.nombre}-postgres"
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = var.clase_rds
  allocated_storage = 20
  storage_encrypted = true
  db_name           = "fuertia"
  username          = "fuertia"
  # RDS genera la contraseña y la guarda en Secrets Manager: nunca pasa por Terraform ni por el repositorio.
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.postgres.name
  vpc_security_group_ids      = [aws_security_group.postgres.id]
  publicly_accessible         = false
  backup_retention_period     = 7
  deletion_protection         = var.proteger_borrado
  skip_final_snapshot         = !var.proteger_borrado
  final_snapshot_identifier   = var.proteger_borrado ? "${var.nombre}-postgres-final" : null
}

resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.nombre}-redis"
  subnet_ids = var.subredes_privadas
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id         = "${var.nombre}-redis"
  engine             = "redis"
  node_type          = var.nodo_redis
  num_cache_nodes    = 1
  port               = 6379
  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]
}

output "postgres_host" { value = aws_db_instance.postgres.address }
output "postgres_secreto_arn" { value = aws_db_instance.postgres.master_user_secret[0].secret_arn }
output "redis_host" { value = aws_elasticache_cluster.redis.cache_nodes[0].address }
