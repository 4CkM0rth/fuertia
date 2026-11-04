package app.fuertia.identidad.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Orígenes (dominios) que pueden llamar a la API desde un navegador.
 * Local: la web con Vite en http://localhost:5173. En AWS se cambia con FUERTIA_CORS_ORIGENES
 * (separados por comas), por ejemplo https://panel.fuertia.app.
 */
@Validated
@ConfigurationProperties(prefix = "fuertia.cors")
public record PropiedadesDeCors(@NotEmpty List<String> origenesPermitidos) {
}
