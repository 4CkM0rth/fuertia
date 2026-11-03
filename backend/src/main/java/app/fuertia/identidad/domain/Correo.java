package app.fuertia.identidad.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Objeto de valor para un correo electrónico.
 *
 * Siempre queda normalizado (sin espacios y en minúsculas) para que "Ana@Correo.com" y
 * "ana@correo.com" sean la misma cuenta. Si no es válido, no se puede construir: así ningún
 * otro código tiene que volver a validarlo.
 */
public record Correo(String valor) {

    // Mismo límite que el contrato de la API y la columna de la base (RFC 5321).
    public static final int LONGITUD_MAXIMA = 254;
    private static final int LONGITUD_MAXIMA_PARTE_LOCAL = 64;

    // Parte local con los caracteres permitidos y un dominio con al menos un punto
    // (rechaza "ana@localhost", que la validación @Email del formulario sí deja pasar).
    private static final Pattern FORMATO = Pattern.compile(
            "^[a-z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$");

    public Correo {
        if (valor == null || valor.isBlank()) {
            throw new CorreoInvalido("Escribe tu correo");
        }
        // Locale.ROOT evita sorpresas con reglas de idioma (por ejemplo la "I" turca).
        valor = valor.strip().toLowerCase(Locale.ROOT);
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new CorreoInvalido("El correo no puede tener más de " + LONGITUD_MAXIMA + " caracteres");
        }
        if (!FORMATO.matcher(valor).matches() || !parteLocalValida(valor)) {
            throw new CorreoInvalido("Escribe un correo válido");
        }
    }

    private static boolean parteLocalValida(String correo) {
        String local = correo.substring(0, correo.indexOf('@'));
        return local.length() <= LONGITUD_MAXIMA_PARTE_LOCAL
                && !local.startsWith(".")
                && !local.endsWith(".")
                && !local.contains("..");
    }

    @Override
    public String toString() {
        return valor;
    }
}
