package app.fuertia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del backend de Fuertia.
 * Cada subpaquete directo (identidad, sistema...) es un módulo con su propia estructura hexagonal.
 * ConfigurationPropertiesScan registra los records con @ConfigurationProperties de todos los módulos.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FuertiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FuertiaApplication.class, args);
    }
}
