package app.fuertia.identidad.application.port.out;

/**
 * Convierte una contraseña en un hash irreversible y la compara después.
 * El algoritmo (hoy Argon2) es un detalle de infraestructura: cambiarlo no toca el caso de uso.
 */
public interface CifradorDeContrasenas {

    String cifrar(String contrasenaEnClaro);

    boolean coincide(String contrasenaEnClaro, String hash);
}
