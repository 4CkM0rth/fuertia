package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.UsuarioId;

public interface ConsultarUsuarioActual {

    /** Busca solo dentro del tenant del token. Lanza SesionInvalida si el usuario no está. */
    UsuarioActual consultar(UsuarioId usuario);
}
