package app.fuertia.identidad.application.port.out;

/**
 * Convierte una contraseña en un hash irreversible y la compara después.
 * El algoritmo (hoy Argon2) es un detalle de infraestructura: cambiarlo no toca el caso de uso.
 */
public interface CifradorDeContrasenas {

    String cifrar(String contrasenaEnClaro);

    boolean coincide(String contrasenaEnClaro, String hash);

    /**
     * Un hash de una contraseña que nadie conoce, calculado con los mismos parámetros que los reales.
     * Cuando el correo no existe se compara contra este, para que la respuesta tarde lo mismo que
     * cuando existe y así el tiempo no revele qué correos tienen cuenta.
     */
    String hashSenuelo();
}
