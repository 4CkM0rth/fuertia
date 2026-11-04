package app.fuertia;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Borra los contadores de límites de peticiones antes de cada prueba de integración.
 *
 * Todas las peticiones de MockMvc salen de la misma IP (127.0.0.1) y el Redis se comparte entre
 * clases: sin esto, la sexta prueba que registra un entrenador recibiría 429 por culpa de las cinco
 * anteriores. Cada prueba arranca con los contadores en cero, como si fuera otra hora.
 */
public class LimpiarLimites implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext contexto) {
        StringRedisTemplate redis = SpringExtension.getApplicationContext(contexto).getBean(StringRedisTemplate.class);
        var claves = redis.keys("fuertia:limite:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }
}
