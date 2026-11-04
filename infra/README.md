# Infraestructura de Fuertia

## Semanas 1 a 6: todo en local

```bash
docker compose up -d
```

| Servicio local | Reemplaza en AWS | Puerto |
| --- | --- | --- |
| PostgreSQL 17 | RDS | 5432 |
| Redis 7 | ElastiCache | 6379 |
| MinIO | S3 | 9000 (API) y 9001 (consola) |

El backend lee todo por variables de entorno (`FUERTIA_DB_URL`, `FUERTIA_REDIS_HOST`, `FUERTIA_ALMACENAMIENTO_*`). En AWS solo cambian los valores; el código es el mismo. Con `FUERTIA_ALMACENAMIENTO_ENDPOINT` vacío, el cliente de S3 habla con AWS. Con `http://localhost:9000`, habla con MinIO.

## Semana 7: AWS con Terraform

`terraform/` está escrito pero **no se ha aplicado**: aplicarlo crea recursos que cuestan plata. El CI solo corre `terraform fmt` y `terraform validate`, que no tocan AWS.

```
terraform/
├── modulos/
│   ├── red/             VPC, 2 subredes públicas y 2 privadas
│   ├── datos/           RDS PostgreSQL y ElastiCache Redis, privados
│   ├── almacenamiento/  buckets privados de fotos (30 días) y PDF
│   ├── servicio/        ECR, ECS Fargate, ALB, roles y registros
│   └── web/             bucket privado y CloudFront para la web
└── ambientes/
    └── pruebas/         usa los módulos con tamaños pequeños
```

Decisiones para que el ambiente de pruebas cueste poco:

- Sin NAT Gateway: las tareas de ECS van en subred pública pero solo aceptan tráfico del balanceador.
- RDS `db.t4g.micro` y Redis `cache.t4g.micro`, una sola copia.
- La contraseña de RDS la genera AWS y queda en Secrets Manager; nunca pasa por el repositorio.

Pasos para la semana 7 (cuando exista la cuenta):

1. Crear el bucket del estado de Terraform y descomentar el bloque `backend "s3"`.
2. `terraform init`, `terraform plan` y revisar el plan con Sebastian antes de `terraform apply`.
3. Agregar el despliegue al CI con un rol de OIDC de GitHub (sin llaves de AWS guardadas).
