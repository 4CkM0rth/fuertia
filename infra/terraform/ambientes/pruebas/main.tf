# Ambiente de pruebas de Fuertia en AWS.
# NO se aplica hasta la semana 7 (cuando exista la cuenta de AWS). Mientras tanto
# todo corre en local con docker-compose.yml, y el CI solo corre terraform validate.

terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
  # En la semana 7: estado remoto en S3 con bloqueo. Por ahora queda comentado.
  # backend "s3" {
  #   bucket       = "fuertia-terraform-estado"
  #   key          = "pruebas/terraform.tfstate"
  #   region       = "us-east-1"
  #   use_lockfile = true
  # }
}

variable "region" {
  type    = string
  default = "us-east-1"
}

provider "aws" {
  region = var.region
  default_tags {
    tags = { Proyecto = "fuertia", Ambiente = "pruebas" }
  }
}

locals {
  nombre = "fuertia-pruebas"
}

module "red" {
  source = "../../modulos/red"
  nombre = local.nombre
}

module "almacenamiento" {
  source = "../../modulos/almacenamiento"
  nombre = local.nombre
}

module "servicio" {
  source            = "../../modulos/servicio"
  nombre            = local.nombre
  region            = var.region
  vpc_id            = module.red.vpc_id
  subredes_publicas = module.red.subredes_publicas
  arns_buckets      = module.almacenamiento.arns
  # Las mismas variables que el backend lee en local; solo cambian los valores.
  variables_entorno = {
    SPRING_PROFILES_ACTIVE          = "pruebas"
    FUERTIA_DB_URL                  = "jdbc:postgresql://${module.datos.postgres_host}:5432/fuertia"
    FUERTIA_REDIS_HOST              = module.datos.redis_host
    FUERTIA_ALMACENAMIENTO_REGION   = var.region
    FUERTIA_ALMACENAMIENTO_FOTOS    = module.almacenamiento.bucket_fotos
    FUERTIA_ALMACENAMIENTO_PDF      = module.almacenamiento.bucket_pdf
    FUERTIA_ALMACENAMIENTO_ENDPOINT = ""
  }
  secretos = {
    # El secreto de RDS es un JSON con username y password.
    FUERTIA_DB_USUARIO    = "${module.datos.postgres_secreto_arn}:username::"
    FUERTIA_DB_CONTRASENA = "${module.datos.postgres_secreto_arn}:password::"
  }
}

module "datos" {
  source            = "../../modulos/datos"
  nombre            = local.nombre
  vpc_id            = module.red.vpc_id
  subredes_privadas = module.red.subredes_privadas
  grupo_backend_id  = module.servicio.grupo_backend_id
}

module "web" {
  source = "../../modulos/web"
  nombre = local.nombre
}

output "url_api" { value = module.servicio.url_balanceador }
output "url_web" { value = module.web.url_web }
output "repositorio_imagen" { value = module.servicio.repositorio_imagen }
