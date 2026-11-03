package app.fuertia.identidad.domain;

/**
 * Un dato no cumple una regla del dominio. Lleva el nombre del campo para que la API
 * pueda devolverlo en el mapa "errores", igual que los errores de validación del formulario.
 */
public class DatoInvalido extends ExcepcionDeDominio {

    private final String campo;

    public DatoInvalido(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
