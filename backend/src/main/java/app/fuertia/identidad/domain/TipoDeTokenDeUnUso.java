package app.fuertia.identidad.domain;

import java.time.Duration;

/** Para qué sirve un enlace de un solo uso, y cuánto dura. */
public enum TipoDeTokenDeUnUso {

    VERIFICACION_CORREO(Duration.ofHours(24)),
    // Corto a propósito: quien tenga acceso al buzón por un rato no debe poder cambiar la clave días después.
    RECUPERACION_CONTRASENA(Duration.ofHours(1));

    private final Duration vigencia;

    TipoDeTokenDeUnUso(Duration vigencia) {
        this.vigencia = vigencia;
    }

    public Duration vigencia() {
        return vigencia;
    }
}
