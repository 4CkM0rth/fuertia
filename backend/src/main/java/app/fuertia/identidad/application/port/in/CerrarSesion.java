package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.SesionId;

public interface CerrarSesion {

    /** Idempotente: cerrar una sesión que no existe o ya está cerrada no falla. */
    void cerrar(SesionId sesion);
}
