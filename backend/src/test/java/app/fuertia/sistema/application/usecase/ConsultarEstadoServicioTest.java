package app.fuertia.sistema.application.usecase;

import app.fuertia.sistema.application.port.out.InformacionDeLaAplicacion;
import app.fuertia.sistema.domain.EstadoDelSistema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarEstadoServicioTest {

    @Test
    void respondeNombreVersionYEstadoOk() {
        InformacionDeLaAplicacion informacion = new InformacionDeLaAplicacion() {
            @Override
            public String nombre() {
                return "Fuertia";
            }

            @Override
            public String version() {
                return "0.1.0";
            }
        };

        EstadoDelSistema estado = new ConsultarEstadoServicio(informacion).consultar();

        assertThat(estado).isEqualTo(new EstadoDelSistema("Fuertia", "0.1.0", "ok"));
    }
}
