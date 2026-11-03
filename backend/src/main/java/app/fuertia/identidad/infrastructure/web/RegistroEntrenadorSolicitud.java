package app.fuertia.identidad.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/identidad/registro, tal como lo define docs/api/openapi.yaml.
 * Los mensajes van escritos aquí (y no en archivos de idioma) para que salgan siempre en español,
 * sin importar el idioma que mande el navegador.
 */
public record RegistroEntrenadorSolicitud(
        @NotBlank(message = "Escribe el nombre de tu negocio")
        @Size(min = 2, max = 80, message = "El nombre del negocio debe tener entre 2 y 80 caracteres")
        String nombreNegocio,

        @NotBlank(message = "Escribe tu nombre")
        @Size(min = 2, max = 80, message = "Tu nombre debe tener entre 2 y 80 caracteres")
        String nombre,

        @NotBlank(message = "Escribe tu correo")
        @Email(message = "Escribe un correo válido")
        @Size(max = 254, message = "El correo no puede tener más de 254 caracteres")
        String correo,

        @NotBlank(message = "Escribe una contraseña")
        @Size(min = 10, max = 128, message = "La contraseña debe tener entre 10 y 128 caracteres")
        String contrasena) {

    @Override
    public String toString() {
        return "RegistroEntrenadorSolicitud[nombreNegocio=" + nombreNegocio + ", nombre=" + nombre
                + ", correo=" + correo + ", contrasena=****]";
    }
}
