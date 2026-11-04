package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.UsuarioId;

import java.time.Instant;
import java.util.Optional;

public interface RepositorioDeSesiones {

    void guardar(Sesion sesion);

    /** Solo dentro del tenant actual. */
    Optional<Sesion> buscarPorId(SesionId id);

    void revocar(SesionId id, Instant ahora);

    /** Cierra todas las sesiones abiertas del usuario (por ejemplo, al cambiar la contraseña). */
    void revocarTodasDe(UsuarioId usuario, Instant ahora);
}
