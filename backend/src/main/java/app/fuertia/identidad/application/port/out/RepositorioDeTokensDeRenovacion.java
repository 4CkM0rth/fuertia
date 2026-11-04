package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TokenDeRenovacion;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeTokensDeRenovacion {

    void guardar(TokenDeRenovacion token);

    /** Solo dentro del tenant actual. */
    Optional<TokenDeRenovacion> buscarPorHash(HashDeToken hash);

    /**
     * Marca el token como usado solo si sigue sin usar. Devuelve false si otra petición lo usó
     * primero; el caso de uso lo trata como reutilización.
     */
    boolean marcarComoUsado(UUID id, Instant ahora);
}
