package app.fuertia.identidad.domain;

/** El correo ya pertenece a una cuenta. El correo es único en todo Fuertia, no solo dentro de un tenant. */
public class CorreoYaRegistrado extends ExcepcionDeDominio {

    private final Correo correo;

    public CorreoYaRegistrado(Correo correo) {
        super("Ya existe una cuenta con ese correo");
        this.correo = correo;
    }

    public Correo correo() {
        return correo;
    }
}
