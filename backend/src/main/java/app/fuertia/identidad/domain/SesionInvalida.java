package app.fuertia.identidad.domain;

/** La sesión no existe, venció o se cerró: hay que iniciar sesión otra vez. */
public class SesionInvalida extends ExcepcionDeDominio {

    public SesionInvalida() {
        super("Tu sesión no es válida. Inicia sesión otra vez.");
    }
}
