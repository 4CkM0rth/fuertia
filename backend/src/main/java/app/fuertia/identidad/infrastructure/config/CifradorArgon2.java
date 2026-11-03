package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Adaptador del puerto CifradorDeContrasenas con Argon2id (primera recomendación de OWASP).
 *
 * Se eligió Argon2 en vez de BCrypt porque BCrypt solo usa los primeros 72 bytes y Spring Security
 * rechaza contraseñas más largas, mientras que el contrato permite hasta 128 caracteres.
 *
 * El DelegatingPasswordEncoder guarda el hash con un prefijo, por ejemplo "{argon2}$argon2id$...".
 * Así, si mañana cambiamos de algoritmo, los hashes viejos se siguen reconociendo.
 */
final class CifradorArgon2 implements CifradorDeContrasenas {

    private static final String ALGORITMO = "argon2";

    private final PasswordEncoder codificador = new DelegatingPasswordEncoder(
            ALGORITMO, Map.of(ALGORITMO, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));

    @Override
    public String cifrar(String contrasenaEnClaro) {
        return codificador.encode(contrasenaEnClaro);
    }

    @Override
    public boolean coincide(String contrasenaEnClaro, String hash) {
        return codificador.matches(contrasenaEnClaro, hash);
    }
}
