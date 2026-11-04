package app.fuertia.identidad.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SHA-256 del token, en hexadecimal (64 caracteres). Es lo único que se guarda en la base.
 *
 * Aquí basta SHA-256 (rápido) y no hace falta Argon2 (lento) como en las contraseñas: el token ya
 * tiene 256 bits de azar, así que no hay un diccionario de "tokens comunes" que probar.
 */
public record HashDeToken(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[0-9a-f]{64}$");

    public HashDeToken {
        Objects.requireNonNull(valor);
        if (!FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("El hash del token debe ser SHA-256 en hexadecimal");
        }
    }

    public static HashDeToken de(TokenSecreto token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.valor().getBytes(StandardCharsets.US_ASCII));
            return new HashDeToken(HexFormat.of().formatHex(resumen));
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM trae SHA-256; si no está, no hay nada razonable que hacer.
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
