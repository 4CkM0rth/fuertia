package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.domain.Canal;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpos de las peticiones de acceso, tal como los define docs/api/openapi.yaml.
 * Los mensajes van escritos aquí para que salgan siempre en español.
 * Los toString esconden los secretos: un record los imprimiría completos.
 */
public final class Solicitudes {

    private Solicitudes() {
    }

    public record TokenDeUnUso(
            @NotBlank(message = "Falta el token del enlace")
            @Size(max = 128, message = "El token no es válido")
            String token) {

        @Override
        public String toString() {
            return "TokenDeUnUso[****]";
        }
    }

    public record InicioDeSesion(
            @NotBlank(message = "Escribe tu correo")
            @Email(message = "Escribe un correo válido")
            @Size(max = 254, message = "El correo no puede tener más de 254 caracteres")
            String correo,

            // Sin mínimo a propósito: decir "muy corta" al iniciar sesión daría pistas sobre la regla.
            @NotBlank(message = "Escribe tu contraseña")
            @Size(max = 128, message = "La contraseña no puede tener más de 128 caracteres")
            String contrasena,

            @NotNull(message = "Indica si entras desde la web o desde la app")
            Canal canal) {

        @Override
        public String toString() {
            return "InicioDeSesion[correo=" + correo + ", contrasena=****, canal=" + canal + "]";
        }
    }

    /** Solo con canal APP. Con canal WEB el token llega en la cookie y no hay cuerpo. */
    public record Renovacion(
            @Size(max = 128, message = "El token no es válido")
            String tokenDeRenovacion) {

        @Override
        public String toString() {
            return "Renovacion[****]";
        }
    }

    public record Recuperacion(
            @NotBlank(message = "Escribe tu correo")
            @Email(message = "Escribe un correo válido")
            @Size(max = 254, message = "El correo no puede tener más de 254 caracteres")
            String correo) {
    }

    public record NuevaContrasena(
            @NotBlank(message = "Falta el token del enlace")
            @Size(max = 128, message = "El token no es válido")
            String token,

            @NotBlank(message = "Escribe una contraseña")
            @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
            String contrasenaNueva) {

        @Override
        public String toString() {
            return "NuevaContrasena[token=****, contrasenaNueva=****]";
        }
    }
}
