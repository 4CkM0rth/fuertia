package app.fuertia;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca una prueba de integración: levanta la app completa contra PostgreSQL y Redis en Docker y
 * un servidor de correo en memoria.
 *
 * Lleva el tag "integracion" para poder excluirla: ./gradlew pruebasRapidas corre todo menos esto.
 * ./gradlew test sí la incluye.
 *
 * Corre con el perfil por defecto (local, llave JWT temporal), pero con la cookie Secure encendida
 * como en producción, para poder comprobar sus atributos. Todas las clases usan exactamente esta
 * configuración, así Spring arranca un solo contexto y unos solos contenedores para todas.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integracion")
@SpringBootTest(properties = "fuertia.identidad.cookie.segura=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(LimpiarLimites.class)
public @interface PruebaDeIntegracion {
}
