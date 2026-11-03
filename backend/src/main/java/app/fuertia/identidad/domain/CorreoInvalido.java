package app.fuertia.identidad.domain;

public class CorreoInvalido extends DatoInvalido {

    public CorreoInvalido(String mensaje) {
        super("correo", mensaje);
    }
}
