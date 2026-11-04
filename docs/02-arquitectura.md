# Arquitectura (aprobada)

## Stack

| Parte | Tecnología |
| --- | --- |
| App | Expo, React Native, Expo Router, EAS Build, TypeScript, almacenamiento seguro, cámara, push de Expo y Apple |
| Web | React, TypeScript, TanStack Query, formularios con validación, librería de gráficas |
| Backend | Java 21 y Spring Boot 4.1, Spring Security (JWT), JPA, Flyway, OpenAPI |
| Datos | PostgreSQL (RDS, red privada) y Redis (colas, caché y límites) |
| Archivos | S3 privado para fotos de comida y para PDF, con URLs firmadas de pocos minutos |
| IA | Gemini con visión (Flash-Lite por defecto) y salida estructurada en JSON. Va detrás de un puerto propio para poder cambiar de proveedor |
| Pagos | Wompi (web), con webhooks firmados |
| Infraestructura | Docker, ECS Fargate, CloudFront y WAF, SES, Terraform, GitHub Actions, CloudWatch y Sentry |
| Pruebas | JUnit 5, Mockito, Testcontainers, ArchUnit y k6 para carga |

## Hexagonal en todos los módulos

Módulos: identidad y acceso, tenants y suscripciones, asesorados y consentimientos, entrenamiento, nutrición e IA, chequeos y progreso, exportación PDF, notificaciones, auditoría.

Estructura de cada módulo (ejemplo: nutrición):

```
nutricion/
├── domain/            entidades y reglas puras (Comida, Porcion, cálculo de macros)
├── application/
│   ├── usecase/       EscanearComida, ConfirmarEscaneo, CrearPlan
│   └── port/
│       ├── in/
│       └── out/       AnalizadorDeComida, RepositorioDeAlimentos, AlmacenDeFotos
└── infrastructure/
    ├── web/           controladores REST y DTO
    ├── persistence/   entidades JPA y repositorios
    ├── ai/            un adaptador por proveedor de IA
    └── config/
```

Reglas que vigila ArchUnit:

- `domain` no importa Spring, JPA ni librerías externas.
- `application` solo conoce `domain` y sus puertos.
- Solo `infrastructure` conoce frameworks, base de datos y proveedores.
- Un módulo no entra al `domain` de otro. Se comunican por puertos de entrada o por eventos.

## Despliegue y escala

- Un solo backend con varias copias en ECS Fargate detrás de un balanceador, sin estado en memoria.
- Workers separados para el escaneo y el PDF, que leen de la cola de Redis y escalan por trabajos pendientes.
- Índices por `tenant_id`, pool de conexiones y réplica de lectura cuando haga falta.
- Caché en Redis para la biblioteca de ejercicios, los alimentos y el plan del día.
- Límite de peticiones por usuario y por tenant.
- Prueba de carga con k6 antes de lanzar.

## Escaneo con IA

1. La app reduce la foto a 1024 px y le quita los datos EXIF.
2. Pide una URL firmada y sube la foto directo a S3.
3. Crea el escaneo; la API lo encola.
4. El worker lee la foto y llama a Gemini con un esquema JSON: alimentos, gramos estimados y confianza.
5. Busca cada alimento en la base y calcula los macros con código.
6. Guarda el resultado como borrador y envía una notificación push.
7. El asesorado confirma o corrige; el entrenador puede revisar.

Estados: Subiendo → EnCola → Analizando → Borrador | Fallido (se puede reintentar) → Confirmado o Corregido → RevisadoPorEntrenador.

- Se guarda la respuesta cruda del proveedor, el modelo y el costo por escaneo.
- Hay límites de escaneos por asesorado y por plan.
- Todo resultado se muestra como estimado, con rango.

Validación antes de construirlo: fotos con gramos que envían los asesorados de Sebastian (los que se puedan conseguir, idealmente de 20 a 30 platos colombianos), más una parte del conjunto público Nutrition5k. Criterio inicial de aprobación: error medio menor al 25% en comidas simples. Si no se cumple, el escaneo sale en una versión más simple (solo identifica alimentos y los gramos se ponen a mano).

## Seguridad y privacidad

- OWASP ASVS y MASVS.
- Cifrado a nivel de columna para los datos de salud (medidas, peso, lesiones).
- Las fotos de comida se borran a los 30 días, salvo que el asesorado decida guardarlas.
- Ley 1581 de Colombia: consentimientos por tipo y versión. Enviar fotos a Gemini es una transmisión internacional de datos y tiene que estar en el consentimiento.
- Auditoría de acciones sensibles.
