# Almacenamiento: buckets privados para fotos de comida y PDF.
# Los mismos nombres de bucket que usa RustFS en docker-compose.yml, con un sufijo por ambiente.

variable "nombre" { type = string }
variable "dias_fotos_temporales" {
  type    = number
  default = 30
}
variable "dias_pdf" {
  type    = number
  default = 30
}

locals {
  buckets = {
    fotos = "${var.nombre}-fotos-comida"
    pdf   = "${var.nombre}-pdf"
  }
}

resource "aws_s3_bucket" "privado" {
  for_each = local.buckets
  bucket   = each.value
}

resource "aws_s3_bucket_public_access_block" "privado" {
  for_each                = aws_s3_bucket.privado
  bucket                  = each.value.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "privado" {
  for_each = aws_s3_bucket.privado
  bucket   = each.value.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Fotos: se borran a los 30 días las que están en temporales/.
# Las que el asesorado decide guardar se mueven a conservadas/ y no vencen.
resource "aws_s3_bucket_lifecycle_configuration" "fotos" {
  bucket = aws_s3_bucket.privado["fotos"].id
  rule {
    id     = "borrar-fotos-temporales"
    status = "Enabled"
    filter {
      prefix = "temporales/"
    }
    expiration {
      days = var.dias_fotos_temporales
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "pdf" {
  bucket = aws_s3_bucket.privado["pdf"].id
  rule {
    id     = "borrar-pdf-viejos"
    status = "Enabled"
    filter {}
    expiration {
      days = var.dias_pdf
    }
  }
}

# La app sube la foto directo a S3 con una URL firmada, así que el bucket acepta PUT desde el navegador y la app.
resource "aws_s3_bucket_cors_configuration" "fotos" {
  bucket = aws_s3_bucket.privado["fotos"].id
  cors_rule {
    allowed_methods = ["PUT"]
    allowed_origins = ["*"]
    allowed_headers = ["content-type"]
    max_age_seconds = 600
  }
}

output "bucket_fotos" { value = aws_s3_bucket.privado["fotos"].bucket }
output "bucket_pdf" { value = aws_s3_bucket.privado["pdf"].bucket }
output "arns" { value = [for b in aws_s3_bucket.privado : b.arn] }
