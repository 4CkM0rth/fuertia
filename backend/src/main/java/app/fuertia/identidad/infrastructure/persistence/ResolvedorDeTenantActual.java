package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.UUID;

/**
 * Le dice a Hibernate cuál es el tenant actual. Hibernate lo llama al abrir cada sesión y luego:
 * - agrega "where tenant_id = ?" a toda consulta sobre entidades con @TenantId (incluido findById);
 * - al insertar, verifica que el tenant_id de la fila sea el de la sesión.
 *
 * Esto es el "filtro global por tenant": nadie tiene que acordarse de filtrar a mano en cada consulta.
 */
public final class ResolvedorDeTenantActual implements CurrentTenantIdentifierResolver<UUID> {

    /**
     * Valor cuando no hay contexto. No coincide con ningún tenant real (TenantId lo rechaza y la
     * base tiene un check), así que no se ve ninguna fila.
     */
    static final UUID SIN_TENANT = TenantId.RESERVADO_SIN_TENANT;

    /** Valor "raíz" de Hibernate: apaga el filtro. Solo se usa dentro de ContextoDeTenant.ejecutarComoSistema. */
    static final UUID SISTEMA = TenantId.RESERVADO_SISTEMA;

    private final ContextoDeTenant contexto;

    public ResolvedorDeTenantActual(ContextoDeTenant contexto) {
        this.contexto = contexto;
    }

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        if (contexto.esSistema()) {
            return SISTEMA;
        }
        return contexto.tenantActual().map(TenantId::valor).orElse(SIN_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return SISTEMA.equals(tenantId);
    }
}
