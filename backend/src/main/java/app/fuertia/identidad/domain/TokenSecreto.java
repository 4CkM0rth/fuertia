package app.fuertia.identidad.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * El valor secreto de un enlace (verificación, recuperación) o de un token de renovación.
 *
 * - 256 bits aleatorios de SecureRandom: imposibles de adivinar, ni probando millones por segundo.
 * - Se escribe en base64url sin relleno (43 caracteres), que va tal cual en una URL.
 * - Solo viaja al usuario (correo, cookie o respuesta). En la base se guarda su hash (HashDeToken),
 *   así un robo de la base no sirve para entrar.
 */
public final class TokenSecreto {

    private static final int BYTES = 32; // 256 bits
    private static final int LONGITUD = 43; // 32 bytes en base64url sin "="
    private static final Pattern FORMATO = Pattern.compile("^[A-Za-z0-9_-]{" + LONGITUD + "}$");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

    private final String valor;

    private TokenSecreto(String valor) {
        this.valor = valor;
    }

    public static TokenSecreto generar() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return new TokenSecreto(CODIFICADOR.encodeToString(bytes));
    }

    /**
     * Lee un token que llegó de afuera. Vacío si no tiene el formato: así ni siquiera se consulta la
     * base con basura y no se calcula el hash de textos enormes.
     */
    public static Optional<TokenSecreto> leer(String texto) {
        if (texto == null || !FORMATO.matcher(texto).matches()) {
            return Optional.empty();
        }
        return Optional.of(new TokenSecreto(texto));
    }

    public String valor() {
        return valor;
    }

    public HashDeToken hash() {
        return HashDeToken.de(this);
    }

    // Nunca se imprime: un toString puede terminar en un log.
    @Override
    public String toString() {
        return "TokenSecreto[****]";
    }
}
