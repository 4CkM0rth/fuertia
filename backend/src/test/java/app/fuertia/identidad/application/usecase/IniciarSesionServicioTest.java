package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.IniciarSesion.Comando;
import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.CorreoSinVerificar;
import app.fuertia.identidad.domain.CredencialesInvalidas;
import app.fuertia.identidad.domain.DemasiadosIntentos;
import app.fuertia.identidad.domain.Usuario;
import org.junit.jupiter.api.Test;

import static app.fuertia.identidad.application.usecase.Escenario.CONTRASENA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IniciarSesionServicioTest {

    private final Escenario e = new Escenario();

    @Test
    void conCorreoYClaveCorrectosAbreUnaSesionNueva() {
        Usuario usuario = e.entrenadorVerificado("beto@correo.com");

        SesionIniciada sesion = e.iniciar.iniciar(new Comando("Beto@Correo.com", CONTRASENA, Canal.APP));

        assertThat(sesion.usuario().id()).isEqualTo(usuario.id());
        assertThat(sesion.usuario().nombreNegocio()).isEqualTo("Negocio verificado");
        assertThat(sesion.canal()).isEqualTo(Canal.APP);
        assertThat(sesion.acceso().valor()).isNotBlank();
        assertThat(e.sesiones.porId.values()).singleElement().satisfies(s -> {
            assertThat(s.usuarioId()).isEqualTo(usuario.id());
            assertThat(s.tenantId()).isEqualTo(usuario.tenantId());
            assertThat(s.canal()).isEqualTo(Canal.APP);
        });
        // Se guarda el hash del token de renovación que se le entrega, nunca el token.
        assertThat(e.tokensDeRenovacion.porId.values()).singleElement()
                .satisfies(t -> assertThat(t.hash()).isEqualTo(sesion.tokenDeRenovacion().hash()));
    }

    @Test
    void conClaveMalaRespondeCredencialesInvalidas() {
        e.entrenadorVerificado("beto@correo.com");

        assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("beto@correo.com", "otra-clave-cualquiera", Canal.WEB)))
                .isInstanceOf(CredencialesInvalidas.class);
        assertThat(e.sesiones.porId).isEmpty();
    }

    @Test
    void conCorreoInexistenteRespondeLoMismoYComparaContraElSenuelo() {
        assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("nadie@correo.com", CONTRASENA, Canal.WEB)))
                .isInstanceOf(CredencialesInvalidas.class)
                .hasMessage("Correo o contraseña incorrectos");
        // El mismo trabajo de Argon2 que con una cuenta real.
        assertThat(e.cifrador.comparadosContra).containsExactly(Dobles.Cifrador.SENUELO);
    }

    @Test
    void sinVerificarYConClaveCorrectaPideConfirmarElCorreo() {
        e.entrenadorSinVerificar("ana@correo.com");

        assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("ana@correo.com", CONTRASENA, Canal.WEB)))
                .isInstanceOf(CorreoSinVerificar.class);
    }

    @Test
    void sinVerificarYConClaveMalaNoRevelaQueLaCuentaExiste() {
        e.entrenadorSinVerificar("ana@correo.com");

        assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("ana@correo.com", "otra-clave-cualquiera", Canal.WEB)))
                .isInstanceOf(CredencialesInvalidas.class);
    }

    @Test
    void alSextoIntentoConElMismoCorreoFrena() {
        for (int i = 0; i < IniciarSesionServicio.MAXIMO_POR_CORREO; i++) {
            assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("nadie@correo.com", "x", Canal.WEB)))
                    .isInstanceOf(CredencialesInvalidas.class);
        }

        assertThatThrownBy(() -> e.iniciar.iniciar(new Comando("NADIE@correo.com", "x", Canal.WEB)))
                .isInstanceOf(DemasiadosIntentos.class);
        // El límite se cuenta antes de Argon2: el sexto intento ni siquiera calcula el hash.
        assertThat(e.cifrador.comparadosContra).hasSize(IniciarSesionServicio.MAXIMO_POR_CORREO);
    }
}
