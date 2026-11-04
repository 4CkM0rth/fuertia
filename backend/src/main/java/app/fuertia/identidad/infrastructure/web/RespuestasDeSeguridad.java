package app.fuertia.identidad.infrastructure.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Las respuestas 401 y 403 que da Spring Security ANTES de llegar a un controlador (sin token, token
 * inválido, rol insuficiente). Van como Problem Details en español, igual que el resto de errores.
 *
 * El JSON se escribe a mano porque los textos son fijos: no hay datos del usuario que escapar.
 */
public final class RespuestasDeSeguridad {

    private static final String NO_AUTENTICADO =
            "{\"type\":\"about:blank\",\"title\":\"Necesitas iniciar sesión\",\"status\":401}";
    private static final String SIN_PERMISO =
            "{\"type\":\"about:blank\",\"title\":\"No tienes permiso para hacer esto\",\"status\":403}";

    private RespuestasDeSeguridad() {
    }

    public static AuthenticationEntryPoint noAutenticado() {
        return (peticion, respuesta, error) -> {
            // Así lo pide el estándar de tokens Bearer (RFC 6750). No se dice POR QUÉ falló el token:
            // esa pista solo le sirve a quien intenta falsificarlo.
            respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            escribir(respuesta, HttpServletResponse.SC_UNAUTHORIZED, NO_AUTENTICADO);
        };
    }

    public static AccessDeniedHandler sinPermiso() {
        return (peticion, respuesta, error) -> escribir(respuesta, HttpServletResponse.SC_FORBIDDEN, SIN_PERMISO);
    }

    private static void escribir(HttpServletResponse respuesta, int estado, String cuerpo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.getWriter().write(cuerpo);
    }
}
