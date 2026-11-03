package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;

import java.util.UUID;

/** Respuesta 201 del registro. Nunca incluye la contraseña ni su hash. */
public record RegistroEntrenadorRespuesta(UUID tenantId, UUID usuarioId, String correo, String rol) {

    static RegistroEntrenadorRespuesta desde(RegistrarEntrenador.Resultado resultado) {
        return new RegistroEntrenadorRespuesta(
                resultado.tenantId().valor(),
                resultado.usuarioId().valor(),
                resultado.correo().valor(),
                resultado.rol().name());
    }
}
