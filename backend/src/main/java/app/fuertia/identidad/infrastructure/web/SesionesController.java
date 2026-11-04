package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.CerrarSesion;
import app.fuertia.identidad.application.port.in.IniciarSesion;
import app.fuertia.identidad.application.port.in.RenovarSesion;
import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.infrastructure.web.Respuestas.SesionIniciadaRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/identidad/sesiones")
class SesionesController {

    private static final Logger log = LoggerFactory.getLogger(SesionesController.class);

    /** Lo exige la renovación con cookie. Un formulario o un enlace de otro sitio no pueden poner este encabezado. */
    static final String ENCABEZADO_CSRF = "X-Fuertia-Csrf";

    private final IniciarSesion iniciarSesion;
    private final RenovarSesion renovarSesion;
    private final CerrarSesion cerrarSesion;
    private final CookieDeRenovacion cookie;
    private final LimitesPorIp limites;
    private final Clock reloj;

    SesionesController(IniciarSesion iniciarSesion, RenovarSesion renovarSesion, CerrarSesion cerrarSesion,
                       CookieDeRenovacion cookie, LimitesPorIp limites, Clock reloj) {
        this.iniciarSesion = iniciarSesion;
        this.renovarSesion = renovarSesion;
        this.cerrarSesion = cerrarSesion;
        this.cookie = cookie;
        this.limites = limites;
        this.reloj = reloj;
    }

    @PostMapping
    ResponseEntity<SesionIniciadaRespuesta> iniciar(@Valid @RequestBody Solicitudes.InicioDeSesion solicitud,
                                                    HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.INICIO_DE_SESION);
        SesionIniciada sesion = iniciarSesion.iniciar(
                new IniciarSesion.Comando(solicitud.correo(), solicitud.contrasena(), solicitud.canal()));
        return responder(sesion);
    }

    /**
     * Canal APP: el token viene en el cuerpo. Canal WEB: viene en la cookie y además se exige el
     * encabezado X-Fuertia-Csrf: 1. Cualquier rechazo responde el mismo 401 y, si había cookie, la borra.
     */
    @PostMapping("/renovacion")
    ResponseEntity<?> renovar(@Valid @RequestBody(required = false) Solicitudes.Renovacion solicitud,
                              @CookieValue(name = CookieDeRenovacion.NOMBRE, required = false) String tokenEnCookie,
                              @RequestHeader(name = ENCABEZADO_CSRF, required = false) String csrf,
                              HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.CON_TOKEN);

        RenovarSesion.Comando comando;
        if (solicitud != null && solicitud.tokenDeRenovacion() != null) {
            comando = new RenovarSesion.Comando(solicitud.tokenDeRenovacion(), Canal.APP);
        } else if (tokenEnCookie != null && !tokenEnCookie.isEmpty()) {
            if (!"1".equals(csrf)) {
                // Sin el encabezado no se toca el token: ni se gasta ni se revoca nada.
                return rechazo(true);
            }
            comando = new RenovarSesion.Comando(tokenEnCookie, Canal.WEB);
        } else {
            return rechazo(false);
        }

        return switch (renovarSesion.renovar(comando)) {
            case RenovarSesion.Renovada renovada -> responder(renovada.sesion());
            case RenovarSesion.Rechazada rechazada -> {
                if (rechazada.motivo() == RenovarSesion.Motivo.REUTILIZADO) {
                    // Sin el token ni datos personales: solo que pasó, para poder investigarlo.
                    log.warn("Se reutilizó un token de renovación: se revocó la sesión completa");
                }
                yield rechazo(comando.canal() == Canal.WEB);
            }
        };
    }

    /** Revoca la sesión del token de acceso (claim sid). Con la web, además borra la cookie. */
    @DeleteMapping("/actual")
    ResponseEntity<Void> cerrar(@AuthenticationPrincipal Jwt jwt) {
        cerrarSesion.cerrar(UsuarioAutenticado.desde(jwt).sesion());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.borrar()).build();
    }

    private ResponseEntity<SesionIniciadaRespuesta> responder(SesionIniciada sesion) {
        var respuesta = ResponseEntity.ok();
        if (sesion.canal() == Canal.WEB) {
            respuesta.header(HttpHeaders.SET_COOKIE, cookie.crear(sesion.tokenDeRenovacion()));
        }
        return respuesta.body(SesionIniciadaRespuesta.desde(sesion, reloj.instant()));
    }

    private ResponseEntity<ProblemDetail> rechazo(boolean borrarCookie) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problema.setTitle("Tu sesión no es válida. Inicia sesión otra vez.");
        var respuesta = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        if (borrarCookie) {
            respuesta.header(HttpHeaders.SET_COOKIE, cookie.borrar());
        }
        return respuesta.body(problema);
    }
}
