package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.ConsultarUsuarioActual;
import app.fuertia.identidad.infrastructure.web.Respuestas.UsuarioActualRespuesta;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identidad")
class UsuarioActualController {

    private final ConsultarUsuarioActual consultarUsuarioActual;

    UsuarioActualController(ConsultarUsuarioActual consultarUsuarioActual) {
        this.consultarUsuarioActual = consultarUsuarioActual;
    }

    /** Cualquier rol con sesión. El usuario sale del token (sub) y se busca dentro de su tenant (tid). */
    @GetMapping("/yo")
    @PreAuthorize("isAuthenticated()")
    public UsuarioActualRespuesta yo(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioActualRespuesta.desde(consultarUsuarioActual.consultar(UsuarioAutenticado.desde(jwt).usuario()));
    }
}
