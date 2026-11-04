package app.fuertia.identidad.domain;

/**
 * El correo no existe o la contraseña no coincide. A propósito no dice cuál de las dos:
 * si lo dijera, cualquiera podría averiguar qué correos tienen cuenta en Fuertia.
 */
public class CredencialesInvalidas extends ExcepcionDeDominio {

    public CredencialesInvalidas() {
        super("Correo o contraseña incorrectos");
    }
}
