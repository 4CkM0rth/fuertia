package app.fuertia.identidad.domain;

/** La contraseña es correcta, pero la persona todavía no abrió el enlace de verificación. */
public class CorreoSinVerificar extends ExcepcionDeDominio {

    public CorreoSinVerificar() {
        super("Confirma tu correo para entrar");
    }
}
