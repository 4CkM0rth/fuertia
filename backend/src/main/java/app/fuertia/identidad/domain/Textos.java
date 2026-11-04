package app.fuertia.identidad.domain;

/** Validaciones de texto que comparten las entidades del módulo. */
final class Textos {

    private Textos() {
    }

    /** Devuelve el texto sin espacios a los lados, o lanza DatoInvalido si no cumple el largo. */
    static String requerido(String valor, String campo, String etiqueta, int minimo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalido(campo, "Escribe " + etiqueta);
        }
        String limpio = valor.strip();
        if (limpio.length() < minimo || limpio.length() > maximo) {
            throw new DatoInvalido(campo, "Debe tener entre " + minimo + " y " + maximo + " caracteres");
        }
        return limpio;
    }
}
