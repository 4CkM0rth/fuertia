package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.application.port.in.UsuarioActual;
import app.fuertia.identidad.application.port.out.EmisorDeTokensDeAcceso;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeRenovacion;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;

import java.time.Instant;
import java.util.Objects;

/**
 * Lo que comparten iniciar y renovar sesión: emitir el siguiente token de renovación de la
 * familia y un token de acceso nuevo.
 */
public final class EmisionDeSesion {

    private final RepositorioDeSesiones sesiones;
    private final RepositorioDeTokensDeRenovacion tokensDeRenovacion;
    private final RepositorioDeTenants tenants;
    private final EmisorDeTokensDeAcceso emisor;

    public EmisionDeSesion(RepositorioDeSesiones sesiones, RepositorioDeTokensDeRenovacion tokensDeRenovacion,
                           RepositorioDeTenants tenants, EmisorDeTokensDeAcceso emisor) {
        this.sesiones = Objects.requireNonNull(sesiones);
        this.tokensDeRenovacion = Objects.requireNonNull(tokensDeRenovacion);
        this.tenants = Objects.requireNonNull(tenants);
        this.emisor = Objects.requireNonNull(emisor);
    }

    /** Abre una sesión nueva (una familia nueva de tokens de renovación). */
    SesionIniciada abrir(Usuario usuario, Canal canal, Instant ahora) {
        Sesion sesion = Sesion.abrir(usuario, canal, ahora);
        sesiones.guardar(sesion);
        return emitir(usuario, sesion, ahora);
    }

    /** Emite el siguiente eslabón de la familia de la sesión y un token de acceso nuevo. */
    SesionIniciada emitir(Usuario usuario, Sesion sesion, Instant ahora) {
        TokenSecreto renovacion = TokenSecreto.generar();
        tokensDeRenovacion.guardar(TokenDeRenovacion.emitir(sesion, renovacion.hash(), ahora));
        var acceso = emisor.emitir(usuario, sesion.id(), ahora);
        Tenant tenant = tenants.buscarPorId(usuario.tenantId())
                .orElseThrow(() -> new IllegalStateException("El usuario no tiene tenant"));
        return new SesionIniciada(acceso, renovacion, sesion.canal(), UsuarioActual.de(usuario, tenant));
    }
}
