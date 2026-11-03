package app.fuertia.identidad.domain;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID valor) {

    public UsuarioId {
        Objects.requireNonNull(valor, "El id del usuario es obligatorio");
    }

    public static UsuarioId nuevo() {
        return new UsuarioId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
