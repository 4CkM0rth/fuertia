package app.fuertia.identidad.domain;

/**
 * Base de los errores de negocio del módulo de identidad.
 * Es Java puro: la capa web decide luego qué código HTTP le corresponde a cada una.
 */
public abstract class ExcepcionDeDominio extends RuntimeException {

    protected ExcepcionDeDominio(String mensaje) {
        super(mensaje);
    }
}
