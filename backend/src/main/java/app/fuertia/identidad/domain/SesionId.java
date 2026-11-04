package app.fuertia.identidad.domain;

import java.util.Objects;
import java.util.UUID;

public record SesionId(UUID valor) {

    public SesionId {
        Objects.requireNonNull(valor, "El id de la sesión es obligatorio");
    }

    public static SesionId nueva() {
        return new SesionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
