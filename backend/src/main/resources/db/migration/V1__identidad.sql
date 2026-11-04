-- Módulo de identidad: tenants (negocios de entrenadores) y usuarios.
-- Flyway corre este archivo una sola vez y anota en flyway_schema_history que ya lo hizo.
-- Nunca se edita después de aplicado: cualquier cambio va en una migración nueva (V2__..., V3__...).

-- Raíz del multi-tenant. No lleva tenant_id porque ella misma es el tenant.
create table tenant (
    id        uuid         primary key,
    nombre    varchar(80)  not null,
    plan      varchar(20)  not null default 'GRATIS',
    creado_en timestamptz  not null default now()
);

create table usuario (
    id              uuid          primary key,
    tenant_id       uuid          not null,
    nombre          varchar(80)   not null,
    correo          varchar(254)  not null,
    contrasena_hash varchar(255)  not null,
    rol             varchar(20)   not null,
    foto_key        varchar(512),
    creado_en       timestamptz   not null default now(),

    constraint fk_usuario_tenant foreign key (tenant_id) references tenant (id),
    -- Único en TODO el sistema, no por tenant: es con lo que la persona inicia sesión.
    constraint uk_usuario_correo unique (correo),
    -- El dominio ya lo guarda en minúsculas; la base lo asegura por si alguien escribe por otro camino.
    constraint ck_usuario_correo_minusculas check (correo = lower(correo)),
    constraint ck_usuario_rol check (rol in ('ENTRENADOR', 'ASESORADO', 'ADMIN'))
);

-- Casi toda consulta filtra por tenant_id (lo agrega Hibernate), así que necesita su índice.
create index ix_usuario_tenant_id on usuario (tenant_id);
