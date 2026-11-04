package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.UsuarioId;
import app.fuertia.identidad.infrastructure.config.FabricaDeJwt;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Quién hace la petición, leído del JWT ya verificado. Es la única fuente del tenant y del rol:
 * nada de esto se toma de la URL, del cuerpo o de un encabezado, que el cliente podría cambiar.
 */
public record UsuarioAutenticado(UsuarioId usuario, TenantId tenant, Rol rol, SesionId sesion) {

    public static UsuarioAutenticado desde(Jwt jwt) {
        return new UsuarioAutenticado(
                new UsuarioId(UUID.fromString(jwt.getSubject())),
                new TenantId(UUID.fromString(jwt.getClaimAsString(FabricaDeJwt.CLAIM_TENANT))),
                Rol.valueOf(jwt.getClaimAsString(FabricaDeJwt.CLAIM_ROL)),
                new SesionId(UUID.fromString(jwt.getClaimAsString(FabricaDeJwt.CLAIM_SESION))));
    }
}
