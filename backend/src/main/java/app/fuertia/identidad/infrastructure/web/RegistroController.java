package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.infrastructure.web.Respuestas.Mensaje;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada HTTP. Solo traduce: JSON a comando, resultado a JSON.
 * No tiene reglas de negocio; esas viven en el caso de uso y en el dominio.
 */
@RestController
@RequestMapping("/api/v1/identidad")
class RegistroController {

    static final String MENSAJE = "Te enviamos un correo. Ábrelo para continuar.";

    private final RegistrarEntrenador registrarEntrenador;
    private final LimitesPorIp limites;

    RegistroController(RegistrarEntrenador registrarEntrenador, LimitesPorIp limites) {
        this.registrarEntrenador = registrarEntrenador;
        this.limites = limites;
    }

    /** Siempre 202 y el mismo mensaje, exista o no el correo: el resultado interno no se muestra. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mensaje registrar(@Valid @RequestBody RegistroEntrenadorSolicitud solicitud, HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.REGISTRO);
        registrarEntrenador.registrar(new RegistrarEntrenador.Comando(
                solicitud.nombreNegocio(), solicitud.nombre(), solicitud.correo(), solicitud.contrasena()));
        return new Mensaje(MENSAJE);
    }
}
