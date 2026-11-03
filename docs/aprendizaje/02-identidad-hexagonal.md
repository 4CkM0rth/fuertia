# 02 · Backend: módulo identidad en hexagonal

Semana 1 · 3 de noviembre de 2026

## 1. Qué se construyó y para qué sirve

El primer módulo del backend, con la estructura que van a copiar todos los demás:

- `POST /api/v1/identidad/registro`: un entrenador nuevo se registra. Se crea su **tenant** (su negocio) y su **usuario** con rol ENTRENADOR y la contraseña cifrada.
- `GET /api/v1/sistema/estado`: lo usan la web y la app para saber que hablan con el backend.
- El **filtro por tenant**: ningún negocio puede ver los datos de otro, aunque el código se equivoque en una consulta.
- **ArchUnit** vigila que nadie rompa la arquitectura.

En la semana 2 se suman login con JWT, verificación de correo, invitaciones y consentimientos sobre esta misma base.

## 2. Archivos y qué hace cada uno

```
backend/src/main/java/app/fuertia/identidad/
├── domain/                     Java puro: las reglas del negocio
│   ├── Correo.java             normaliza a minúsculas y valida el formato
│   ├── Usuario.java, Tenant.java, Rol.java, Plan.java
│   ├── TenantId.java, UsuarioId.java     ids como records (no se confunde uno con otro)
│   ├── PoliticaDeContrasena.java         mínimo 10 caracteres, máximo 128
│   └── CorreoYaRegistrado.java, CorreoInvalido.java, DatoInvalido.java
├── application/
│   ├── port/in/RegistrarEntrenador.java  lo que el módulo ofrece
│   ├── port/out/                         lo que necesita de afuera
│   │   ├── RepositorioDeUsuarios.java
│   │   ├── RepositorioDeTenants.java
│   │   └── CifradorDeContrasenas.java
│   └── usecase/RegistrarEntrenadorServicio.java   el caso de uso
└── infrastructure/
    ├── web/          RegistroController, DTO y errores en español (Problem Details)
    ├── persistence/  entidades JPA, adaptadores y el filtro por tenant
    ├── config/       SecurityConfig, CifradorArgon2, wiring de Spring
    └── ai/           vacío a propósito: este módulo no usa IA
```

| Otro archivo | Qué hace |
| --- | --- |
| `db/migration/V1__identidad.sql` | Crea las tablas `tenant` y `usuario` |
| `application.yml` | Configuración por variables de entorno, con valores por defecto iguales a Docker Compose |
| `ArquitecturaTest.java` | Las 5 reglas de ArchUnit |
| `AislamientoPorTenantIntegracionTest.java` | Prueba que el tenant A no ve los datos del tenant B |

## 3. Conceptos nuevos

**Caso de uso.** Una acción del negocio, en Java puro. `RegistrarEntrenadorServicio` crea el `Correo`, valida la contraseña, revisa que el correo no exista, crea el tenant y el usuario y los guarda. No tiene ninguna anotación de Spring, y por eso su prueba corre en milisegundos.

**Puerto.** Una interfaz en el idioma del negocio. El caso de uso dice "necesito un `RepositorioDeUsuarios`" sin saber si detrás hay PostgreSQL o una lista en memoria. En la prueba se usa `UsuariosEnMemoria`, unas pocas líneas que cumplen el mismo puerto.

**Adaptador.** La pieza que conecta un puerto con una tecnología. `RepositorioDeUsuariosJpa` lo hace con JPA y `CifradorArgon2` con Argon2. `RegistroController` es un adaptador de entrada: convierte JSON en un comando del caso de uso. Si mañana cambia el algoritmo de cifrado, solo se toca `CifradorArgon2`.

**ArchUnit.** Pruebas que leen el código compilado y verifican las reglas de la arquitectura: el `domain` solo usa Java, la `application` solo conoce el `domain`, y un módulo no entra al `domain` de otro. Si alguien pone `@Component` en el dominio, la prueba falla. `backend-dev` lo comprobó rompiendo una regla a propósito: fallaron las 4 reglas que debían fallar.

**Flyway.** Versiona la base de datos como si fuera código. Al arrancar, corre las migraciones `V1__`, `V2__`, etc. que falten y las anota en `flyway_schema_history`. **Una migración aplicada nunca se edita**: los cambios van en una nueva. Hibernate, con `ddl-auto: validate`, solo revisa que las entidades coincidan con las tablas.

**Testcontainers.** Levanta un PostgreSQL y un Redis reales en Docker solo mientras corren las pruebas. `@ServiceConnection` le pasa a Spring la dirección que asignó Docker. Se prueba contra la base de verdad y no contra una imitación. Por eso hace falta Docker encendido.

**Multi-tenant con `@TenantId`.** `UsuarioEntidad` marca su columna `tenant_id` con `@TenantId`. En cada sesión, Hibernate le pregunta a `ResolvedorDeTenantActual` cuál es el tenant y agrega el filtro a **todas** las consultas, incluido `findById`. Además rechaza guardar un usuario con otro tenant. Tiene tres modos:

- **Dentro de un tenant:** lo normal.
- **Sin contexto:** no se ve nada. Esto se llama *fallar cerrado*: ante la duda, no mostrar.
- **Como sistema:** el filtro se apaga. Solo lo usa el registro, porque es justamente quien crea el tenant.

**Por qué la búsqueda de correo es global.** Al iniciar sesión todavía no se sabe a qué tenant pertenece la persona, así que el correo tiene que ser único en todo el sistema. Es la única consulta sin filtro, y está escrita en SQL nativo a propósito, con un comentario que explica el porqué.

**Problem Details (RFC 9457).** Un formato estándar para los errores de una API. Un 409 se ve así:

```json
{ "title": "Ya existe una cuenta con ese correo", "status": 409 }
```

## 4. Decisiones y alternativas descartadas

| Decisión | Por qué | Descartado |
| --- | --- | --- |
| Argon2id para las contraseñas | BCrypt solo usa 72 bytes y el contrato permite hasta 128 caracteres | BCrypt |
| Filtro con `@TenantId` de Hibernate | Se aplica solo, incluso si alguien olvida el `where` | Escribir `where tenant_id = ?` a mano en cada consulta |
| Una base con `tenant_id` en cada tabla | Simple y barata para cientos de entrenadores | Una base o un esquema por tenant |
| `/error` y `/actuator/health/**` públicos | Sin `/error`, un 400 de una ruta pública salía como 401. Las sondas de ECS necesitan health | Dejarlos cerrados |
| Tenant dentro de identidad por ahora | Todavía no existe el módulo de suscripciones | Crear ya el módulo tenants vacío |
| Pruebas con tag `integracion` | Se pueden correr solo las rápidas mientras programas | Una sola tarea de pruebas |

## 5. Cómo probarlo tú mismo

1. Prende Docker Desktop.
2. Pruebas rápidas (no necesitan Docker): `cd backend` y luego `.\gradlew.bat pruebasRapidas`.
3. Todas, incluidas las de integración: `.\gradlew.bat test`.
4. Levanta el backend con la base local:
   ```bash
   docker compose up -d
   ```
   ```bash
   cd backend && ./gradlew bootRun
   ```
5. En otra terminal:
   ```bash
   curl http://localhost:8080/api/v1/sistema/estado
   ```
   ```bash
   curl -X POST http://localhost:8080/api/v1/identidad/registro -H "Content-Type: application/json" -d '{"nombreNegocio":"Mi Negocio","nombre":"Sebastian","correo":"yo@ejemplo.com","contrasena":"una-clave-segura"}'
   ```
   Repite el segundo comando y deberías ver el 409.
6. Rompe ArchUnit a propósito: en `identidad/domain/Usuario.java` agrega `import org.springframework.stereotype.Component;` y `@Component` sobre la clase. Corre `pruebasRapidas` y mira qué regla falla. Luego devuélvelo.

## 6. Para confirmar que lo entendiste

1. ¿Por qué `RegistrarEntrenadorServicioTest` no necesita Spring ni Docker, y `RegistroIntegracionTest` sí?
2. Si en la semana 3 alguien escribe `ejercicioRepositorio.findAll()` y olvida filtrar por tenant, ¿qué pasa? ¿Qué pieza lo protege?
3. Ejercicio: quieres guardar el teléfono del entrenador. ¿En qué archivos irías, en qué orden y por qué no puedes editar `V1__identidad.sql`?
