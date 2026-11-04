package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.CerrarSesion;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.domain.SesionId;

import java.time.Clock;
import java.util.Objects;

public final class CerrarSesionServicio implements CerrarSesion {

    private final RepositorioDeSesiones sesiones;
    private final Clock reloj;

    public CerrarSesionServicio(RepositorioDeSesiones sesiones, Clock reloj) {
        this.sesiones = Objects.requireNonNull(sesiones);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void cerrar(SesionId sesion) {
        // Revocar la sesión deja sin efecto toda su familia de tokens de renovación. El token de
        // acceso ya emitido sigue vivo hasta que venza (máximo 15 minutos).
        sesiones.revocar(Objects.requireNonNull(sesion), reloj.instant());
    }
}
