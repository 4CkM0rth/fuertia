package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.in.VerificarCorreo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST y no GET a propósito: algunos lectores de correo y antivirus abren solos los enlaces para
 * revisarlos. Si el enlace verificara con un GET, ese robot gastaría el token. La web recibe el
 * token en /verificar?token=... y es ella la que hace este POST.
 */
@RestController
@RequestMapping("/api/v1/identidad")
class VerificacionController {

    private final VerificarCorreo verificarCorreo;
    private final LimitesPorIp limites;

    VerificacionController(VerificarCorreo verificarCorreo, LimitesPorIp limites) {
        this.verificarCorreo = verificarCorreo;
        this.limites = limites;
    }

    @PostMapping("/verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verificar(@Valid @RequestBody Solicitudes.TokenDeUnUso solicitud, HttpServletRequest peticion) {
        limites.consumir(peticion, LimitesPorIp.Accion.CON_TOKEN);
        verificarCorreo.verificar(solicitud.token());
    }
}
