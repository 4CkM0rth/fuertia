package app.fuertia.identidad.infrastructure.config;

/**
 * Ya hay demasiados cálculos de Argon2 en curso y no se liberó un turno a tiempo.
 * La API responde 503 con Retry-After: el servidor está sano, solo saturado por un momento.
 */
public class CifradorOcupado extends RuntimeException {

    public CifradorOcupado() {
        super("Demasiados cálculos de contraseña al mismo tiempo");
    }
}
