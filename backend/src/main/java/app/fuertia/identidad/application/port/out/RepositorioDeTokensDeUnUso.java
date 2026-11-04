package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.UsuarioId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeTokensDeUnUso {

    void guardar(TokenDeUnUso token);

    /** Solo dentro del tenant actual. */
    Optional<TokenDeUnUso> buscarPorHash(HashDeToken hash);

    /**
     * Marca el token como usado solo si nadie lo usó antes. Devuelve false si otra petición lo usó
     * primero: así dos clics al mismo enlace no lo usan dos veces.
     */
    boolean marcarComoUsado(UUID id, Instant ahora);

    /** Deja sin efecto los enlaces de ese tipo que el usuario aún no usó. */
    void invalidarPendientes(UsuarioId usuario, TipoDeTokenDeUnUso tipo, Instant ahora);
}
