package app.fuertia.sistema.infrastructure.web;

import app.fuertia.sistema.domain.EstadoDelSistema;

/** Esquema EstadoSistema de docs/api/openapi.yaml. */
public record EstadoRespuesta(String nombre, String version, String estado) {

    static EstadoRespuesta desde(EstadoDelSistema estado) {
        return new EstadoRespuesta(estado.nombre(), estado.version(), estado.estado());
    }
}
