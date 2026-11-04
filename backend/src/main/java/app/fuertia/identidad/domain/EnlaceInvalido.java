package app.fuertia.identidad.domain;

/** El enlace no existe, ya se usó o venció. Se responde igual en los tres casos. */
public class EnlaceInvalido extends ExcepcionDeDominio {

    public EnlaceInvalido() {
        super("El enlace no es válido o ya venció");
    }
}
