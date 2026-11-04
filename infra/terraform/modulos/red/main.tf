# Red: una VPC con dos subredes públicas (balanceador y tareas de ECS)
# y dos privadas (RDS y Redis), en dos zonas de disponibilidad.

variable "nombre" { type = string }
variable "cidr" {
  type    = string
  default = "10.20.0.0/16"
}

data "aws_availability_zones" "disponibles" {
  state = "available"
}

locals {
  zonas = slice(data.aws_availability_zones.disponibles.names, 0, 2)
}

resource "aws_vpc" "principal" {
  cidr_block           = var.cidr
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = var.nombre }
}

resource "aws_internet_gateway" "principal" {
  vpc_id = aws_vpc.principal.id
  tags   = { Name = var.nombre }
}

resource "aws_subnet" "publica" {
  count                   = 2
  vpc_id                  = aws_vpc.principal.id
  cidr_block              = cidrsubnet(var.cidr, 8, count.index)
  availability_zone       = local.zonas[count.index]
  map_public_ip_on_launch = true
  tags                    = { Name = "${var.nombre}-publica-${count.index}" }
}

resource "aws_subnet" "privada" {
  count             = 2
  vpc_id            = aws_vpc.principal.id
  cidr_block        = cidrsubnet(var.cidr, 8, count.index + 10)
  availability_zone = local.zonas[count.index]
  tags              = { Name = "${var.nombre}-privada-${count.index}" }
}

resource "aws_route_table" "publica" {
  vpc_id = aws_vpc.principal.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.principal.id
  }
  tags = { Name = "${var.nombre}-publica" }
}

resource "aws_route_table_association" "publica" {
  count          = 2
  subnet_id      = aws_subnet.publica[count.index].id
  route_table_id = aws_route_table.publica.id
}

# Las subredes privadas no tienen salida a internet: RDS y Redis no la necesitan.

output "vpc_id" { value = aws_vpc.principal.id }
output "subredes_publicas" { value = aws_subnet.publica[*].id }
output "subredes_privadas" { value = aws_subnet.privada[*].id }
