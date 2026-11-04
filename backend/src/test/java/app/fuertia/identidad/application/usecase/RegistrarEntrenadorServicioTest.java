package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.in.RegistrarEntrenador.Comando;
import app.fuertia.identidad.domain.CorreoInvalido;
import app.fuertia.identidad.domain.DatoInvalido;
import app.fuertia.identidad.domain.Plan;
import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba del caso de uso sin Spring ni base de datos, con los dobles de Dobles. */
class RegistrarEntrenadorServicioTest {

    private final Escenario e = new Escenario();

    @Test
    void conCorreoNuevoCreaTenantYEntrenadorSinVerificar() {
        var resultado = e.registrar.registrar(comando("Sebastian.Rojas@Ejemplo.com"));

        assertThat(resultado).isInstanceOf(RegistrarEntrenador.CuentaCreada.class);
        var creada = (RegistrarEntrenador.CuentaCreada) resultado;
        assertThat(e.tenants.porId.values()).singleElement().satisfies(tenant -> {
            assertThat(tenant.id()).isEqualTo(creada.tenantId());
            assertThat(tenant.nombre()).isEqualTo("Sebastian Rojas Coaching");
            assertThat(tenant.plan()).isEqualTo(Plan.GRATIS);
        });
        assertThat(e.usuarios.porId.values()).singleElement().satisfies(usuario -> {
            assertThat(usuario.tenantId()).isEqualTo(creada.tenantId());
            assertThat(usuario.rol()).isEqualTo(Rol.ENTRENADOR);
            assertThat(usuario.verificado()).isFalse();
            assertThat(usuario.contrasenaHash()).isEqualTo("cifrada(una-clave-segura)");
            assertThat(usuario.correo().valor()).isEqualTo("sebastian.rojas@ejemplo.com");
        });
    }

    @Test
    void conCorreoNuevoEmiteUnEnlaceDeVerificacionDe24HorasYLoEnvia() {
        e.registrar.registrar(comando("ana@correo.com"));

        TokenDeUnUso token = e.tokensDeUnUso.porId.values().iterator().next();
        assertThat(token.tipo()).isEqualTo(TipoDeTokenDeUnUso.VERIFICACION_CORREO);
        assertThat(Duration.between(token.creadoEn(), token.venceEn())).isEqualTo(Duration.ofHours(24));
        assertThat(e.correos.enviados).singleElement().satisfies(correo -> {
            assertThat(correo.tipo()).isEqualTo("verificacion");
            assertThat(correo.destinatario().valor()).isEqualTo("ana@correo.com");
            // Lo que se guarda es el hash del mismo secreto que viaja en el correo.
            assertThat(correo.token().hash()).isEqualTo(token.hash());
        });
    }

    @Test
    void conCorreoExistenteNoCreaNadaYEnviaElAviso() {
        e.registrar.registrar(comando("ana@correo.com"));

        var resultado = e.registrar.registrar(comando("ANA@Correo.com"));

        assertThat(resultado).isInstanceOf(RegistrarEntrenador.CuentaExistente.class);
        assertThat(e.tenants.porId).hasSize(1);
        assertThat(e.usuarios.porId).hasSize(1);
        assertThat(e.tokensDeUnUso.porId).hasSize(1);
        assertThat(e.correos.enviados).extracting(Dobles.Correos.Enviado::tipo)
                .containsExactly("verificacion", "cuenta-existente");
    }

    @Test
    void cifraLaContrasenaEnLosDosCaminosParaQueTardenLoMismo() {
        e.registrar.registrar(comando("ana@correo.com"));
        assertThat(e.cifrador.cifrados).isEqualTo(1);

        e.registrar.registrar(comando("ana@correo.com"));
        assertThat(e.cifrador.cifrados).isEqualTo(2);
    }

    @Test
    void rechazaUnCorreoInvalidoSinGuardarNiEnviarNada() {
        assertThatThrownBy(() -> e.registrar.registrar(comando("no-es-correo"))).isInstanceOf(CorreoInvalido.class);
        assertThat(e.usuarios.porId).isEmpty();
        assertThat(e.correos.enviados).isEmpty();
    }

    @Test
    void rechazaUnaContrasenaCortaTambienSiElCorreoYaExiste() {
        e.registrar.registrar(comando("ana@correo.com"));

        assertThatThrownBy(() -> e.registrar.registrar(new Comando("Negocio", "Ana", "ana@correo.com", "corta")))
                .isInstanceOfSatisfying(DatoInvalido.class, error -> assertThat(error.campo()).isEqualTo("contrasena"));
    }

    @Test
    void elComandoNoMuestraLaContrasenaEnSuToString() {
        assertThat(comando("ana@correo.com").toString()).doesNotContain("una-clave-segura");
    }

    private static Comando comando(String correo) {
        return new Comando("Sebastian Rojas Coaching", "Sebastian Rojas", correo, "una-clave-segura");
    }
}
