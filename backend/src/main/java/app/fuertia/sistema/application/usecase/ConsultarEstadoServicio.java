package app.fuertia.sistema.application.usecase;

import app.fuertia.sistema.application.port.in.ConsultarEstado;
import app.fuertia.sistema.application.port.out.InformacionDeLaAplicacion;
import app.fuertia.sistema.domain.EstadoDelSistema;

import java.util.Objects;

public final class ConsultarEstadoServicio implements ConsultarEstado {

    private final InformacionDeLaAplicacion informacion;

    public ConsultarEstadoServicio(InformacionDeLaAplicacion informacion) {
        this.informacion = Objects.requireNonNull(informacion);
    }

    @Override
    public EstadoDelSistema consultar() {
        return EstadoDelSistema.operativo(informacion.nombre(), informacion.version());
    }
}
