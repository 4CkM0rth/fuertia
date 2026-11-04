package app.fuertia;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Servicios reales para las pruebas de integración: PostgreSQL y Redis en Docker (las mismas
 * imágenes que docker-compose.yml) y un servidor de correo.
 *
 * @ServiceConnection le pasa a Spring la URL, el usuario y el puerto que Docker asignó, así no hay
 * que escribir propiedades a mano. Spring reutiliza el mismo contexto (y los mismos contenedores)
 * entre clases de prueba con la misma configuración, lo que ahorra arrancarlos cada vez.
 *
 * Correo con GreenMail y no con un contenedor de Mailpit: GreenMail es un servidor SMTP dentro de
 * la misma JVM, arranca en milisegundos y deja leer los correos recibidos con una llamada Java, sin
 * consultar una API HTTP. Como nuestro adaptador solo habla SMTP, para él no hay diferencia.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    // Los límites de peticiones viven en Redis, y /actuator/health también lo revisa.
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }

    /** Servidor SMTP de prueba en un puerto libre que elige el sistema operativo. */
    @Bean(initMethod = "start", destroyMethod = "stop")
    GreenMail servidorDeCorreo() {
        return new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    }

    /** Apunta spring.mail al GreenMail de arriba (el puerto solo se sabe cuando ya arrancó). */
    @Bean
    DynamicPropertyRegistrar propiedadesDeCorreo(GreenMail servidorDeCorreo) {
        return propiedades -> {
            propiedades.add("spring.mail.host", () -> servidorDeCorreo.getSmtp().getBindTo());
            propiedades.add("spring.mail.port", () -> servidorDeCorreo.getSmtp().getPort());
        };
    }
}
