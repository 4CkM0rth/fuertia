package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.IniciarSesion;
import app.fuertia.identidad.application.port.in.RenovarSesion;
import app.fuertia.identidad.application.port.in.RenovarSesion.Comando;
import app.fuertia.identidad.application.port.in.RenovarSesion.Motivo;
import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import app.fuertia.identidad.domain.TokenSecreto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static app.fuertia.identidad.application.usecase.Escenario.CONTRASENA;
import static org.assertj.core.api.Assertions.assertThat;

class RenovarSesionServicioTest {

    private final Escenario e = new Escenario();
    private SesionIniciada inicial;

    @BeforeEach
    void iniciarSesion() {
        e.entrenadorVerificado("beto@correo.com");
        inicial = e.iniciar.iniciar(new IniciarSesion.Comando("beto@correo.com", CONTRASENA, Canal.APP));
    }

    @Test
    void entregaUnTokenNuevoYGastaElAnterior() {
        SesionIniciada segunda = renovada(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)));

        assertThat(segunda.tokenDeRenovacion().valor()).isNotEqualTo(inicial.tokenDeRenovacion().valor());
        // Misma sesión (misma familia), dos eslabones: el primero usado y el segundo vigente.
        assertThat(e.sesiones.porId).hasSize(1);
        assertThat(e.tokensDeRenovacion.porId.values())
                .extracting(t -> t.estado(e.reloj.instant()))
                .containsExactly(TokenDeRenovacion.Estado.USADO, TokenDeRenovacion.Estado.VIGENTE);

        // Y el nuevo sirve para la siguiente renovación.
        assertThat(e.renovar.renovar(new Comando(segunda.tokenDeRenovacion().valor(), Canal.APP)))
                .isInstanceOf(RenovarSesion.Renovada.class);
    }

    @Test
    void siSeReutilizaUnTokenRevocaTodaLaFamilia() {
        SesionIniciada segunda = renovada(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)));

        // Alguien vuelve a usar el primero: señal de que hay una copia.
        assertThat(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.REUTILIZADO));
        assertThat(e.sesiones.porId.values()).allMatch(s -> !s.estaActiva());

        // El token más nuevo de la familia también quedó sin efecto.
        assertThat(e.renovar.renovar(new Comando(segunda.tokenDeRenovacion().valor(), Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.SESION_CERRADA));
    }

    @Test
    void unTokenVencidoSeRechazaSinRevocar() {
        e.reloj.adelantar(TokenDeRenovacion.VIGENCIA.plus(Duration.ofSeconds(1)));

        assertThat(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.VENCIDO));
        assertThat(e.sesiones.porId.values()).allMatch(Sesion::estaActiva);
    }

    @Test
    void porElCanalEquivocadoSeRechazaSinGastarElToken() {
        assertThat(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.WEB)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.CANAL_EQUIVOCADO));

        assertThat(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)))
                .isInstanceOf(RenovarSesion.Renovada.class);
    }

    @Test
    void despuesDeCerrarSesionElTokenNoSirve() {
        e.cerrar.cerrar(e.sesiones.porId.keySet().iterator().next());

        assertThat(e.renovar.renovar(new Comando(inicial.tokenDeRenovacion().valor(), Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.SESION_CERRADA));
    }

    @Test
    void unTokenInventadoOMalFormadoNoExiste() {
        assertThat(e.renovar.renovar(new Comando(TokenSecreto.generar().valor(), Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.NO_EXISTE));
        assertThat(e.renovar.renovar(new Comando("basura", Canal.APP)))
                .isEqualTo(new RenovarSesion.Rechazada(Motivo.NO_EXISTE));
    }

    private static SesionIniciada renovada(RenovarSesion.Resultado resultado) {
        assertThat(resultado).isInstanceOf(RenovarSesion.Renovada.class);
        return ((RenovarSesion.Renovada) resultado).sesion();
    }
}
