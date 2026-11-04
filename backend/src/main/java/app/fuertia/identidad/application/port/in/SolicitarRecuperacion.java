package app.fuertia.identidad.application.port.in;

public interface SolicitarRecuperacion {

    /** No dice si la cuenta existe: si existe envía el enlace, si no, no hace nada. */
    void solicitar(String correo);
}
