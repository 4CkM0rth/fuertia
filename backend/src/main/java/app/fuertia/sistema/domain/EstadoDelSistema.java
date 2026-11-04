package app.fuertia.sistema.domain;

import java.util.Objects;

/** Foto del estado del backend: cómo se llama, qué versión corre y si está operativo. */
public record EstadoDelSistema(String nombre, String version, String estado) {

    public static final String OPERATIVO = "ok";

    public EstadoDelSistema {
        Objects.requireNonNull(nombre);
        Objects.requireNonNull(version);
        Objects.requireNonNull(estado);
    }

    /** Si el proceso alcanza a responder, está operativo. La salud de la base y Redis la mide /actuator/health. */
    public static EstadoDelSistema operativo(String nombre, String version) {
        return new EstadoDelSistema(nombre, version, OPERATIVO);
    }
}
