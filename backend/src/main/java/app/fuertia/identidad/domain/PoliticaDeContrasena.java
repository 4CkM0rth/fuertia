package app.fuertia.identidad.domain;

/**
 * Regla de negocio de la contraseña en claro, antes de cifrarla.
 * El formulario ya la valida, pero el dominio la repite: otro adaptador (un script, la app)
 * podría llamar al caso de uso sin pasar por la API.
 */
public final class PoliticaDeContrasena {

    public static final int LONGITUD_MINIMA = 10;
    public static final int LONGITUD_MAXIMA = 128;

    private PoliticaDeContrasena() {
    }

    public static void validar(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new DatoInvalido("contrasena", "Escribe una contraseña");
        }
        if (contrasena.length() < LONGITUD_MINIMA || contrasena.length() > LONGITUD_MAXIMA) {
            throw new DatoInvalido("contrasena",
                    "La contraseña debe tener entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA + " caracteres");
        }
    }
}
