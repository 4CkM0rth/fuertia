package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Arma todos los casos de uso con los mismos dobles y un reloj que se puede adelantar.
 * Cada prueba crea el suyo, así no comparten estado.
 */
final class Escenario {

    /** Reloj de prueba: siempre marca la misma hora hasta que la prueba lo adelanta. */
    static final class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-11-02T15:00:00Z");

        void adelantar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }

    final Reloj reloj = new Reloj();
    final Dobles.Usuarios usuarios = new Dobles.Usuarios();
    final Dobles.Tenants tenants = new Dobles.Tenants();
    final Dobles.TokensDeUnUso tokensDeUnUso = new Dobles.TokensDeUnUso();
    final Dobles.Sesiones sesiones = new Dobles.Sesiones();
    final Dobles.TokensDeRenovacion tokensDeRenovacion = new Dobles.TokensDeRenovacion();
    final Dobles.Cifrador cifrador = new Dobles.Cifrador();
    final Dobles.Correos correos = new Dobles.Correos();
    final Dobles.Limitador limitador = new Dobles.Limitador();
    final EmisionDeSesion emision = new EmisionDeSesion(sesiones, tokensDeRenovacion, tenants, new Dobles.Emisor());

    final RegistrarEntrenadorServicio registrar =
            new RegistrarEntrenadorServicio(usuarios, tenants, tokensDeUnUso, cifrador, correos, reloj);
    final VerificarCorreoServicio verificar = new VerificarCorreoServicio(tokensDeUnUso, usuarios, reloj);
    final IniciarSesionServicio iniciar = new IniciarSesionServicio(usuarios, cifrador, limitador, emision, reloj);
    final RenovarSesionServicio renovar = new RenovarSesionServicio(tokensDeRenovacion, sesiones, usuarios, emision, reloj);
    final CerrarSesionServicio cerrar = new CerrarSesionServicio(sesiones, reloj);
    final SolicitarRecuperacionServicio solicitarRecuperacion =
            new SolicitarRecuperacionServicio(usuarios, tokensDeUnUso, correos, limitador, reloj);
    final ConfirmarRecuperacionServicio confirmarRecuperacion =
            new ConfirmarRecuperacionServicio(tokensDeUnUso, usuarios, sesiones, cifrador, reloj);
    final ConsultarUsuarioActualServicio consultarUsuarioActual = new ConsultarUsuarioActualServicio(usuarios, tenants);

    static final String CONTRASENA = "una-clave-segura";

    /** Registra un entrenador sin verificar y devuelve su usuario. */
    Usuario entrenadorSinVerificar(String correo) {
        var creada = (RegistrarEntrenador.CuentaCreada) registrar.registrar(
                new RegistrarEntrenador.Comando("Negocio de prueba", "Ana Pérez", correo, CONTRASENA));
        return usuarios.buscarPorId(creada.usuarioId()).orElseThrow();
    }

    /** Un entrenador ya verificado, guardado directo en los dobles. */
    Usuario entrenadorVerificado(String correo) {
        Tenant tenant = Tenant.nuevo("Negocio verificado", reloj.instant());
        tenants.guardar(tenant);
        Usuario usuario = Usuario.nuevoEntrenador(tenant.id(), "Beto Gómez", new Correo(correo),
                cifrador.cifrar(CONTRASENA), reloj.instant()).conCorreoVerificado();
        usuarios.guardar(usuario);
        return usuario;
    }
}
