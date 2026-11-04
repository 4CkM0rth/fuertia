package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.ConfirmarRecuperacion.Comando;
import app.fuertia.identidad.application.port.in.IniciarSesion;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.DatoInvalido;
import app.fuertia.identidad.domain.DemasiadosIntentos;
import app.fuertia.identidad.domain.EnlaceInvalido;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static app.fuertia.identidad.application.usecase.Escenario.CONTRASENA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecuperacionServicioTest {

    private static final String NUEVA = "clave-nueva-segura";

    private final Escenario e = new Escenario();

    @Test
    void conCuentaEnviaUnEnlaceDeUnaHora() {
        e.entrenadorVerificado("beto@correo.com");

        e.solicitarRecuperacion.solicitar("Beto@correo.com");

        TokenDeUnUso token = e.tokensDeUnUso.porId.values().iterator().next();
        assertThat(token.tipo()).isEqualTo(TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA);
        assertThat(Duration.between(token.creadoEn(), token.venceEn())).isEqualTo(Duration.ofHours(1));
        assertThat(e.correos.enviados).singleElement()
                .satisfies(correo -> assertThat(correo.tipo()).isEqualTo("recuperacion"));
    }

    @Test
    void sinCuentaNoHaceNadaYNoFalla() {
        e.solicitarRecuperacion.solicitar("nadie@correo.com");

        assertThat(e.tokensDeUnUso.porId).isEmpty();
        assertThat(e.correos.enviados).isEmpty();
    }

    @Test
    void elLimiteSeAplicaAunqueLaCuentaNoExista() {
        for (int i = 0; i < SolicitarRecuperacionServicio.MAXIMO_POR_CORREO; i++) {
            e.solicitarRecuperacion.solicitar("nadie@correo.com");
        }
        assertThatThrownBy(() -> e.solicitarRecuperacion.solicitar("nadie@correo.com"))
                .isInstanceOf(DemasiadosIntentos.class);
    }

    @Test
    void confirmarCambiaLaClaveYCierraTodasLasSesiones() {
        Usuario usuario = e.entrenadorVerificado("beto@correo.com");
        e.iniciar.iniciar(new IniciarSesion.Comando("beto@correo.com", CONTRASENA, Canal.WEB));
        e.iniciar.iniciar(new IniciarSesion.Comando("beto@correo.com", CONTRASENA, Canal.APP));
        e.solicitarRecuperacion.solicitar("beto@correo.com");

        e.confirmarRecuperacion.confirmar(new Comando(tokenEnviado().valor(), NUEVA));

        assertThat(e.usuarios.buscarPorId(usuario.id()).orElseThrow().contrasenaHash()).isEqualTo("cifrada(" + NUEVA + ")");
        assertThat(e.sesiones.porId).hasSize(2);
        assertThat(e.sesiones.porId.values()).noneMatch(s -> s.estaActiva());
    }

    @Test
    void confirmarTambienVerificaElCorreo() {
        Usuario usuario = e.entrenadorSinVerificar("ana@correo.com");
        e.solicitarRecuperacion.solicitar("ana@correo.com");

        e.confirmarRecuperacion.confirmar(new Comando(tokenEnviado().valor(), NUEVA));

        assertThat(e.usuarios.buscarPorId(usuario.id()).orElseThrow().verificado()).isTrue();
    }

    @Test
    void elEnlaceNoSirveDosVecesNiDespuesDeUnaHora() {
        e.entrenadorVerificado("beto@correo.com");
        e.solicitarRecuperacion.solicitar("beto@correo.com");
        String token = tokenEnviado().valor();
        e.confirmarRecuperacion.confirmar(new Comando(token, NUEVA));

        assertThatThrownBy(() -> e.confirmarRecuperacion.confirmar(new Comando(token, NUEVA)))
                .isInstanceOf(EnlaceInvalido.class);

        e.solicitarRecuperacion.solicitar("beto@correo.com");
        e.reloj.adelantar(Duration.ofHours(1));
        assertThatThrownBy(() -> e.confirmarRecuperacion.confirmar(new Comando(tokenEnviado().valor(), NUEVA)))
                .isInstanceOf(EnlaceInvalido.class);
    }

    @Test
    void pedirOtroEnlaceAnulaElAnterior() {
        e.entrenadorVerificado("beto@correo.com");
        e.solicitarRecuperacion.solicitar("beto@correo.com");
        String primero = tokenEnviado().valor();
        e.solicitarRecuperacion.solicitar("beto@correo.com");

        assertThatThrownBy(() -> e.confirmarRecuperacion.confirmar(new Comando(primero, NUEVA)))
                .isInstanceOf(EnlaceInvalido.class);
        e.confirmarRecuperacion.confirmar(new Comando(tokenEnviado().valor(), NUEVA));
    }

    @Test
    void unaClaveNuevaCortaNoGastaElEnlace() {
        e.entrenadorVerificado("beto@correo.com");
        e.solicitarRecuperacion.solicitar("beto@correo.com");
        String token = tokenEnviado().valor();

        assertThatThrownBy(() -> e.confirmarRecuperacion.confirmar(new Comando(token, "corta")))
                .isInstanceOf(DatoInvalido.class);
        e.confirmarRecuperacion.confirmar(new Comando(token, NUEVA));
    }

    @Test
    void unEnlaceDeVerificacionNoSirveParaCambiarLaClave() {
        e.entrenadorSinVerificar("ana@correo.com");

        assertThatThrownBy(() -> e.confirmarRecuperacion.confirmar(new Comando(tokenEnviado().valor(), NUEVA)))
                .isInstanceOf(EnlaceInvalido.class);
    }

    private TokenSecreto tokenEnviado() {
        return e.correos.enviados.getLast().token();
    }
}
