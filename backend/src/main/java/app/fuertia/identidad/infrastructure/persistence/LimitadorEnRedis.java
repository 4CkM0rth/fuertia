package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import app.fuertia.identidad.domain.DemasiadosIntentos;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/**
 * Adaptador del puerto LimitadorDePeticiones con Redis: una ventana fija por clave.
 *
 * Cada intento hace INCR de la clave; el primero le pone la duración con PEXPIRE. Cuando la clave
 * vence, el contador vuelve a cero. Se hace con un script de Lua porque Redis lo ejecuta de una
 * sola vez: si el INCR y el PEXPIRE fueran dos llamadas y el proceso se cayera entre las dos, la
 * clave quedaría sin vencimiento y esa IP bloqueada para siempre.
 *
 * Si Redis no responde, la excepción sube y la petición falla (falla cerrado): preferimos no dejar
 * pasar intentos sin contar. De todos modos /actuator/health ya marcaría el servicio como caído.
 */
@Component
class LimitadorEnRedis implements LimitadorDePeticiones {

    private static final String PREFIJO = "fuertia:limite:";

    private static final RedisScript<List> CONTAR = new DefaultRedisScript<>("""
            local intentos = redis.call('INCR', KEYS[1])
            if intentos == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return {intentos, redis.call('PTTL', KEYS[1])}
            """, List.class);

    private final StringRedisTemplate redis;

    LimitadorEnRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void consumir(String clave, int maximo, Duration ventana) {
        @SuppressWarnings("unchecked")
        List<Long> resultado = redis.execute(CONTAR, List.of(PREFIJO + resumir(clave)),
                String.valueOf(ventana.toMillis()));
        long intentos = resultado.get(0);
        long faltaMs = resultado.get(1);
        if (intentos > maximo) {
            throw new DemasiadosIntentos(Duration.ofMillis(faltaMs > 0 ? faltaMs : ventana.toMillis()));
        }
    }

    // Las claves llevan correos e IP, que son datos personales: en Redis se guarda solo su SHA-256.
    private static String resumir(String clave) {
        int separador = clave.lastIndexOf(':');
        String tipo = separador >= 0 ? clave.substring(0, separador) : "general";
        String dato = separador >= 0 ? clave.substring(separador + 1) : clave;
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(dato.getBytes(StandardCharsets.UTF_8));
            return tipo + ":" + HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
