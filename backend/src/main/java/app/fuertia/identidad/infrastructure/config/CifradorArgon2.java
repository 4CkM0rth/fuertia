package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    // Parámetros mínimos que recomienda OWASP para Argon2id: 19 MiB de memoria (19456 KiB),
    // 2 iteraciones y 1 hilo. Sal de 16 bytes y hash de 32 bytes. Los valores por defecto de
    // Spring (16 MiB) quedan por debajo de ese mínimo. Subirlos hace más caro cada intento de
    // adivinar contraseñas si alguien roba la base, a cambio de unos milisegundos por inicio de sesión.
    private static final int LONGITUD_SAL = 16;
    private static final int LONGITUD_HASH = 32;
    private static final int PARALELISMO = 1;
    private static final int MEMORIA_KIB = 19456;
    private static final int ITERACIONES = 2;

    private final PasswordEncoder codificador = new DelegatingPasswordEncoder(
            ALGORITMO, Map.of(ALGORITMO,
                    new Argon2PasswordEncoder(LONGITUD_SAL, LONGITUD_HASH, PARALELISMO, MEMORIA_KIB, ITERACIONES)));

    // Semáforo: como una puerta con N fichas. Cada cálculo toma una y la devuelve al terminar; si no
    // quedan, espera. Así nunca hay más de N cálculos de 19 MiB a la vez, aunque lleguen 500 intentos.
    // "true" = justo: atiende en orden de llegada.
    private final Semaphore permisos;
    private final Duration espera;
    private final String hashSenuelo;

    CifradorArgon2(int simultaneos, Duration espera) {
        this.permisos = new Semaphore(simultaneos, true);
        this.espera = espera;
        // Con los mismos parámetros que los hashes reales, para que compararlo cueste lo mismo.
        // La contraseña es aleatoria y se descarta: nadie puede "acertarle" al señuelo.
        this.hashSenuelo = codificador.encode(UUID.randomUUID().toString());
    }

    @Override
    public String cifrar(String contrasenaEnClaro) {
        return conPermiso(() -> codificador.encode(contrasenaEnClaro));
    }

    @Override
    public boolean coincide(String contrasenaEnClaro, String hash) {
        return conPermiso(() -> codificador.matches(contrasenaEnClaro, hash));
    }

    @Override
    public String hashSenuelo() {
        return hashSenuelo;
    }

    private <T> T conPermiso(Supplier<T> calculo) {
        boolean obtenido;
        try {
            obtenido = permisos.tryAcquire(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CifradorOcupado();
        }
        if (!obtenido) {
            throw new CifradorOcupado();
        }
        try {
            return calculo.get();
        } finally {
            permisos.release();
        }
    }
}
