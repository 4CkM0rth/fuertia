package app.fuertia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Contenedores reales de PostgreSQL y Redis para las pruebas de integración, con las mismas
 * imágenes que docker-compose.yml.
 *
 * @ServiceConnection le pasa a Spring la URL, el usuario y el puerto que Docker asignó, así no hay
 * que escribir propiedades a mano. Spring reutiliza el mismo contexto (y los mismos contenedores)
 * entre clases de prueba con la misma configuración, lo que ahorra arrancarlos cada vez.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    // Spring Data Redis no se conecta al arrancar, pero /actuator/health sí revisa Redis.
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }
}
