package app.fuertia.identidad.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un tenant (el negocio de un entrenador).
 * Usar un tipo propio en vez de un UUID suelto evita confundirlo con un UsuarioId:
 * el compilador no deja pasar uno donde va el otro.
 */
public record TenantId(UUID valor) {

    public TenantId {
        Objects.requireNonNull(valor, "El id del tenant es obligatorio");
    }

    public static TenantId nuevo() {
        return new TenantId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
