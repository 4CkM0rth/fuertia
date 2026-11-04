package app.fuertia.identidad.application.port.in;

public interface VerificarCorreo {

    /** Lanza EnlaceInvalido si el token no existe, venció o ya se usó. */
    void verificar(String token);
}
