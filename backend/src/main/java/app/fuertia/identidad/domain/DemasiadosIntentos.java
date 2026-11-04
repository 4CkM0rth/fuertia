package app.fuertia.identidad.domain;

import java.time.Duration;
import java.util.Objects;

/** Se pasó el límite de intentos. Lleva cuánto hay que esperar, para el encabezado Retry-After. */
public class DemasiadosIntentos extends ExcepcionDeDominio {

    private final Duration reintentarEn;

    public DemasiadosIntentos(Duration reintentarEn) {
        super("Hiciste demasiados intentos. Espera un momento y vuelve a probar.");
        this.reintentarEn = Objects.requireNonNull(reintentarEn);
    }

    public Duration reintentarEn() {
        return reintentarEn;
    }
}
