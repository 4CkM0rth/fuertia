package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Pone el tenant del JWT en ContextoDeTenant para todo lo que pase en esta petición.
 *
 * Va en la cadena de Spring Security justo después del filtro que verifica el JWT, y antes de que
 * se llegue al controlador. Así el tenant ya está puesto cuando el caso de uso abre su transacción,
 * que es cuando Hibernate lo lee. El try-with-resources lo quita al terminar, pase lo que pase,
 * para que el hilo vuelva limpio al grupo de hilos del servidor.
 *
 * No es un @Component a propósito: si lo fuera, Spring Boot también lo registraría como filtro
 * normal del servidor y correría dos veces. Lo crea SecurityConfig.
 */
public final class FiltroDeTenantDelToken extends OncePerRequestFilter {

    private final ContextoDeTenant contexto;

    public FiltroDeTenantDelToken(ContextoDeTenant contexto) {
        this.contexto = contexto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            UsuarioAutenticado usuario = UsuarioAutenticado.desde(jwt.getToken());
            try (var alcance = contexto.entrarEnTenant(usuario.tenant())) {
                cadena.doFilter(peticion, respuesta);
            }
        } else {
            cadena.doFilter(peticion, respuesta);
        }
    }
}
