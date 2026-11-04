-- Acceso del entrenador: verificación del correo, enlaces de un solo uso y sesiones.
-- Toda tabla nueva lleva tenant_id (lo filtra Hibernate) y su índice.

-- Las cuentas creadas antes de esta migración quedan sin verificar. Solo existen en bases locales
-- de desarrollo; quien tenga una puede usar "recuperar contraseña", que también la verifica.
alter table usuario add column verificado boolean not null default false;

-- Permite que las tablas hijas apunten a (id, tenant_id) y no solo a id: así la base garantiza
-- que un token o una sesión siempre tiene el MISMO tenant que su usuario.
alter table usuario add constraint uk_usuario_id_tenant unique (id, tenant_id);

-- Enlaces de un solo uso: verificar el correo (24 horas) y recuperar la contraseña (1 hora).
-- Se guarda el SHA-256 del token, nunca el token.
create table token_de_un_uso (
    id         uuid         primary key,
    tenant_id  uuid         not null,
    usuario_id uuid         not null,
    tipo       varchar(30)  not null,
    hash       varchar(64)  not null,
    creado_en  timestamptz  not null,
    vence_en   timestamptz  not null,
    usado_en   timestamptz,

    constraint fk_token_de_un_uso_usuario foreign key (usuario_id, tenant_id)
        references usuario (id, tenant_id) on delete cascade,
    -- Único en todo el sistema: el enlace llega sin sesión y se busca solo por su hash.
    constraint uk_token_de_un_uso_hash unique (hash),
    constraint ck_token_de_un_uso_tipo check (tipo in ('VERIFICACION_CORREO', 'RECUPERACION_CONTRASENA')),
    constraint ck_token_de_un_uso_hash check (hash ~ '^[0-9a-f]{64}$'),
    constraint ck_token_de_un_uso_vigencia check (vence_en > creado_en),
    constraint ck_token_de_un_uso_uso check (usado_en is null or usado_en >= creado_en),
    constraint ck_token_de_un_uso_tenant_no_reservado
        check (tenant_id not in ('00000000-0000-0000-0000-000000000000'::uuid,
                                 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid))
);

create index ix_token_de_un_uso_tenant_id on token_de_un_uso (tenant_id);
create index ix_token_de_un_uso_usuario on token_de_un_uso (usuario_id, tipo);

-- Una sesión por dispositivo. Agrupa la familia de tokens de renovación.
create table sesion (
    id          uuid         primary key,
    tenant_id   uuid         not null,
    usuario_id  uuid         not null,
    canal       varchar(10)  not null,
    creada_en   timestamptz  not null,
    revocada_en timestamptz,

    constraint fk_sesion_usuario foreign key (usuario_id, tenant_id)
        references usuario (id, tenant_id) on delete cascade,
    constraint uk_sesion_id_tenant unique (id, tenant_id),
    constraint ck_sesion_canal check (canal in ('WEB', 'APP')),
    constraint ck_sesion_revocada check (revocada_en is null or revocada_en >= creada_en),
    constraint ck_sesion_tenant_no_reservado
        check (tenant_id not in ('00000000-0000-0000-0000-000000000000'::uuid,
                                 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid))
);

create index ix_sesion_tenant_id on sesion (tenant_id);
-- Para cerrar todas las sesiones abiertas de un usuario al cambiar la contraseña.
create index ix_sesion_usuario_abiertas on sesion (usuario_id) where revocada_en is null;

-- Tokens de renovación: 30 días, un solo uso, rotan en cada renovación. Todos los de una sesión
-- forman una familia; si uno usado vuelve a llegar, se revoca la sesión entera.
create table token_de_renovacion (
    id        uuid         primary key,
    tenant_id uuid         not null,
    sesion_id uuid         not null,
    hash      varchar(64)  not null,
    creado_en timestamptz  not null,
    vence_en  timestamptz  not null,
    usado_en  timestamptz,

    constraint fk_token_de_renovacion_sesion foreign key (sesion_id, tenant_id)
        references sesion (id, tenant_id) on delete cascade,
    -- Único en todo el sistema: al renovar todavía no se sabe el tenant, se busca por el hash.
    constraint uk_token_de_renovacion_hash unique (hash),
    constraint ck_token_de_renovacion_hash check (hash ~ '^[0-9a-f]{64}$'),
    constraint ck_token_de_renovacion_vigencia check (vence_en > creado_en),
    constraint ck_token_de_renovacion_uso check (usado_en is null or usado_en >= creado_en),
    constraint ck_token_de_renovacion_tenant_no_reservado
        check (tenant_id not in ('00000000-0000-0000-0000-000000000000'::uuid,
                                 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid))
);

create index ix_token_de_renovacion_tenant_id on token_de_renovacion (tenant_id);
create index ix_token_de_renovacion_sesion on token_de_renovacion (sesion_id);
