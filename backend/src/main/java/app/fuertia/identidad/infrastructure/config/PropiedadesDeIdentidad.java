package app.fuertia.identidad.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Configuración del módulo de identidad (prefijo fuertia.identidad). Los valores salen de
 * variables de entorno; ver application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "fuertia.identidad")
public record PropiedadesDeIdentidad(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cookie cookie,
        @Valid @NotNull Correo correo,
        @Valid @NotNull Argon2 argon2) {

    /** llavePrivada: PKCS8 PEM de una llave EC P-256. Vacía solo se acepta en el perfil local. */
    public record Jwt(String llavePrivada) {

        @Override
        public String toString() {
            return "Jwt[llavePrivada=****]";
        }
    }

    /** segura=false solo en local, donde se trabaja con http y no con https. */
    public record Cookie(boolean segura) {
    }

    /** urlWeb: dónde está la web, para armar los enlaces de los correos. */
    public record Correo(@NotNull URI urlWeb, @NotBlank String remitente) {
    }

    /**
     * Cuántos cálculos de Argon2 pueden correr a la vez. Cada uno usa 19 MiB de RAM y un núcleo por
     * decenas de milisegundos: sin este tope, una ráfaga de inicios de sesión agota la memoria.
     */
    public record Argon2(@Min(1) int simultaneos, @NotNull Duration espera) {
    }
}
