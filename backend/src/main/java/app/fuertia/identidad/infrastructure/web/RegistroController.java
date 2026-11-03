package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
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

    private final RegistrarEntrenador registrarEntrenador;

    RegistroController(RegistrarEntrenador registrarEntrenador) {
        this.registrarEntrenador = registrarEntrenador;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    RegistroEntrenadorRespuesta registrar(@Valid @RequestBody RegistroEntrenadorSolicitud solicitud) {
        var resultado = registrarEntrenador.registrar(new RegistrarEntrenador.Comando(
                solicitud.nombreNegocio(), solicitud.nombre(), solicitud.correo(), solicitud.contrasena()));
        return RegistroEntrenadorRespuesta.desde(resultado);
    }
}
