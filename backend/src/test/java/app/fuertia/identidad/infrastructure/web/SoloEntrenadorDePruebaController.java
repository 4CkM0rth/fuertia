package app.fuertia.identidad.infrastructure.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint que SOLO existe en las pruebas (vive en src/test): todavía no hay endpoints de
 * entrenador, pero hay que demostrar desde ya que @PreAuthorize deja fuera a un ASESORADO.
 * Cuando existan los reales, se protegen igual: @PreAuthorize("hasRole('ENTRENADOR')").
 */
@RestController
class SoloEntrenadorDePruebaController {

    static final String RUTA = "/api/v1/pruebas/solo-entrenador";

    @GetMapping(RUTA)
    @PreAuthorize("hasRole('ENTRENADOR')")
    public Map<String, String> soloEntrenador() {
        return Map.of("resultado", "ok");
    }
}
