package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.domain.TokenDeRenovacion;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.infrastructure.config.PropiedadesDeIdentidad;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * La cookie que guarda el token de renovación en el navegador (canal WEB).
 *
 * - HttpOnly: el JavaScript de la página no la puede leer. Si alguien logra meter un script en la
 *   web (XSS), no se puede llevar el token de 30 días.
 * - Secure: solo viaja por https. Se apaga únicamente en local, donde todo es http.
 * - SameSite=Strict: el navegador no la manda si la petición la inicia otro sitio.
 * - Path=/api/v1/identidad/sesiones: solo viaja a renovar y cerrar sesión, no en cada petición.
 */
@Component
class CookieDeRenovacion {

    static final String NOMBRE = "fuertia_renovacion";
    static final String RUTA = "/api/v1/identidad/sesiones";

    private final boolean segura;

    CookieDeRenovacion(PropiedadesDeIdentidad propiedades) {
        this.segura = propiedades.cookie().segura();
    }

    String crear(TokenSecreto token) {
        return base(token.valor()).maxAge(TokenDeRenovacion.VIGENCIA).build().toString();
    }

    /** Misma cookie con duración cero: el navegador la borra. */
    String borrar() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(segura)
                .sameSite("Strict")
                .path(RUTA);
    }
}
