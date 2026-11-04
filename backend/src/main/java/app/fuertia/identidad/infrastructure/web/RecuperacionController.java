package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.ConfirmarRecuperacion;
import app.fuertia.identidad.application.port.in.SolicitarRecuperacion;
import app.fuertia.identidad.infrastructure.web.Respuestas.Mensaje;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identidad/recuperacion")
class RecuperacionController {

    static final String MENSAJE = "Si hay una cuenta con ese correo, te enviamos un enlace para cambiar la contraseña.";

    private final SolicitarRecuperacion solicitarRecuperacion;
    private final ConfirmarRecuperacion confirmarRecuperacion;
    private final LimitesPorIp limites;

    RecuperacionController(SolicitarRecuperacion solicitarRecuperacion, ConfirmarRecuperacion confirmarRecuperacion,
                           LimitesPorIp limites) {
        this.solicitarRecuperacion = solicitarRecuperacion;
        this.confirmarRecuperacion = confirmarRecuperacion;
        this.limites = limites;
    }

    /** Siempre 202 con el mismo mensaje: no revela si la cuenta existe. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mensaje solicitar(@Valid @RequestBody Solicitudes.Recuperacion solicitud, HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.RECUPERACION);
        solicitarRecuperacion.solicitar(solicitud.correo());
        return new Mensaje(MENSAJE);
    }

    @PostMapping("/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmar(@Valid @RequestBody Solicitudes.NuevaContrasena solicitud, HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.CON_TOKEN);
        confirmarRecuperacion.confirmar(new ConfirmarRecuperacion.Comando(solicitud.token(), solicitud.contrasenaNueva()));
    }
}
