package app.fuertia;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una prueba de integración: levanta la app completa contra PostgreSQL y Redis en Docker.
 *
 * Lleva el tag "integracion" para poder excluirla: ./gradlew pruebasRapidas corre todo menos esto.
 * ./gradlew test sí la incluye.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integracion")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public @interface PruebaDeIntegracion {
}
