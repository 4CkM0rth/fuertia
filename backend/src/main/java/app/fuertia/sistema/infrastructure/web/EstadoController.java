package app.fuertia.sistema.infrastructure.web;

import app.fuertia.sistema.application.port.in.ConsultarEstado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público. La web y la app lo llaman para comprobar que hablan con el backend. */
@RestController
@RequestMapping("/api/v1/sistema")
class EstadoController {

    private final ConsultarEstado consultarEstado;

    EstadoController(ConsultarEstado consultarEstado) {
        this.consultarEstado = consultarEstado;
    }

    @GetMapping("/estado")
    EstadoRespuesta estado() {
        return EstadoRespuesta.desde(consultarEstado.consultar());
    }
}
