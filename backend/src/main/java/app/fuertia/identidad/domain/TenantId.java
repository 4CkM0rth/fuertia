package app.fuertia.identidad.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un tenant (el negocio de un entrenador).
 * Usar un tipo propio en vez de un UUID suelto evita confundirlo con un UsuarioId:
 * el compilador no deja pasar uno donde va el otro.
 */
public record TenantId(UUID valor) {

    /**
     * Valores reservados que nunca pueden ser un tenant real. El filtro por tenant los usa como
     * centinelas: "sin tenant" (no ve nada) y "sistema" (apaga el filtro). Si un tenant real tuviera
     * uno de estos ids, quedaría invisible o, peor, vería los datos de todos. Por eso se rechazan
     * aquí y también en la base (V2__restricciones_tenant.sql).
     */
    public static final UUID RESERVADO_SIN_TENANT = new UUID(0L, 0L);
    public static final UUID RESERVADO_SISTEMA = new UUID(-1L, -1L);

    public TenantId {
        Objects.requireNonNull(valor, "El id del tenant es obligatorio");
        if (RESERVADO_SIN_TENANT.equals(valor) || RESERVADO_SISTEMA.equals(valor)) {
            throw new IllegalArgumentException("El id " + valor + " está reservado y no puede ser un tenant");
        }
    }

    public static TenantId nuevo() {
        return new TenantId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
