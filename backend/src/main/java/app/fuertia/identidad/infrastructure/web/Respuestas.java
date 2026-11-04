package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.application.port.in.UsuarioActual;
import app.fuertia.identidad.domain.Canal;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Cuerpos de las respuestas de acceso (docs/api/openapi.yaml). */
public final class Respuestas {

    private Respuestas() {
    }

    public record Mensaje(String mensaje) {
    }

    public record UsuarioActualRespuesta(UUID id, UUID tenantId, String nombre, String correo, String rol,
                                  String nombreNegocio) {

        static UsuarioActualRespuesta desde(UsuarioActual usuario) {
            return new UsuarioActualRespuesta(usuario.id().valor(), usuario.tenantId().valor(), usuario.nombre(),
                    usuario.correo().valor(), usuario.rol().name(), usuario.nombreNegocio());
        }
    }

    /** tokenDeRenovacion solo aparece con canal APP; con canal WEB va en la cookie y el campo se omite. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SesionIniciadaRespuesta(String tokenDeAcceso, long venceEnSegundos, String tokenDeRenovacion,
                                   UsuarioActualRespuesta usuario) {

        static SesionIniciadaRespuesta desde(SesionIniciada sesion, Instant ahora) {
            long segundos = Math.max(0, Duration.between(ahora, sesion.acceso().venceEn()).toSeconds());
            String renovacion = sesion.canal() == Canal.APP ? sesion.tokenDeRenovacion().valor() : null;
            return new SesionIniciadaRespuesta(sesion.acceso().valor(), segundos, renovacion,
                    UsuarioActualRespuesta.desde(sesion.usuario()));
        }

        @Override
        public String toString() {
            return "SesionIniciadaRespuesta[****]";
        }
    }
}
