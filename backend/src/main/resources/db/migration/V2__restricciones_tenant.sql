-- Los ids 00000000-0000-0000-0000-000000000000 y ffffffff-ffff-ffff-ffff-ffffffffffff están
-- reservados: el filtro por tenant (ResolvedorDeTenantActual) los usa como "sin tenant" y "sistema".
-- Un tenant real con uno de esos ids quedaría invisible o se saltaría el filtro, así que la base
-- los rechaza aunque alguien escriba por fuera de la aplicación. El dominio (TenantId) también.

alter table tenant
    add constraint ck_tenant_id_no_reservado
        check (id not in ('00000000-0000-0000-0000-000000000000'::uuid,
                          'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid));

alter table usuario
    add constraint ck_usuario_tenant_id_no_reservado
        check (tenant_id not in ('00000000-0000-0000-0000-000000000000'::uuid,
                                 'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid));
