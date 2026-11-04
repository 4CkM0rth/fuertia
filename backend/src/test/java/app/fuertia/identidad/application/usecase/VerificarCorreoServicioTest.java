package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.domain.EnlaceInvalido;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificarCorreoServicioTest {

    private final Escenario e = new Escenario();

    @Test
    void conElEnlaceDelCorreoQuedaVerificado() {
        Usuario usuario = e.entrenadorSinVerificar("ana@correo.com");

        e.verificar.verificar(tokenEnviado().valor());

        assertThat(e.usuarios.buscarPorId(usuario.id()).orElseThrow().verificado()).isTrue();
    }

    @Test
    void elMismoEnlaceNoSirveDosVeces() {
        e.entrenadorSinVerificar("ana@correo.com");
        String token = tokenEnviado().valor();
        e.verificar.verificar(token);

        assertThatThrownBy(() -> e.verificar.verificar(token)).isInstanceOf(EnlaceInvalido.class);
    }

    @Test
    void unEnlaceVencidoNoSirve() {
        Usuario usuario = e.entrenadorSinVerificar("ana@correo.com");
        e.reloj.adelantar(Duration.ofHours(24));

        assertThatThrownBy(() -> e.verificar.verificar(tokenEnviado().valor())).isInstanceOf(EnlaceInvalido.class);
        assertThat(e.usuarios.buscarPorId(usuario.id()).orElseThrow().verificado()).isFalse();
    }

    @Test
    void unTokenInventadoOConFormatoMaloNoSirve() {
        e.entrenadorSinVerificar("ana@correo.com");

        assertThatThrownBy(() -> e.verificar.verificar(TokenSecreto.generar().valor())).isInstanceOf(EnlaceInvalido.class);
        assertThatThrownBy(() -> e.verificar.verificar("corto")).isInstanceOf(EnlaceInvalido.class);
        assertThatThrownBy(() -> e.verificar.verificar(null)).isInstanceOf(EnlaceInvalido.class);
    }

    @Test
    void unEnlaceDeRecuperacionNoSirveParaVerificar() {
        Usuario usuario = e.entrenadorVerificado("beto@correo.com");
        e.solicitarRecuperacion.solicitar(usuario.correo().valor());

        assertThatThrownBy(() -> e.verificar.verificar(tokenEnviado().valor())).isInstanceOf(EnlaceInvalido.class);
    }

    private TokenSecreto tokenEnviado() {
        return e.correos.enviados.getLast().token();
    }
}
