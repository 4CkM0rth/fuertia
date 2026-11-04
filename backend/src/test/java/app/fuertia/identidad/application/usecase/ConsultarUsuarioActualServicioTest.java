package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.domain.SesionInvalida;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarUsuarioActualServicioTest {

    private final Escenario e = new Escenario();

    @Test
    void devuelveLosDatosConElNombreDelNegocio() {
        Usuario usuario = e.entrenadorVerificado("beto@correo.com");

        var actual = e.consultarUsuarioActual.consultar(usuario.id());

        assertThat(actual.id()).isEqualTo(usuario.id());
        assertThat(actual.tenantId()).isEqualTo(usuario.tenantId());
        assertThat(actual.nombreNegocio()).isEqualTo("Negocio verificado");
        assertThat(actual.correo().valor()).isEqualTo("beto@correo.com");
    }

    @Test
    void siElUsuarioNoEstaLaSesionNoEsValida() {
        assertThatThrownBy(() -> e.consultarUsuarioActual.consultar(UsuarioId.nuevo()))
                .isInstanceOf(SesionInvalida.class);
    }
}
