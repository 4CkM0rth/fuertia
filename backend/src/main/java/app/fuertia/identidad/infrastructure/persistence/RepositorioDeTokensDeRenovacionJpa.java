package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeRenovacion;
import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class RepositorioDeTokensDeRenovacionJpa implements RepositorioDeTokensDeRenovacion {

    private final TokenDeRenovacionJpaRepositorio jpa;
    private final ContextoDeTenant contexto;

    RepositorioDeTokensDeRenovacionJpa(TokenDeRenovacionJpaRepositorio jpa, ContextoDeTenant contexto) {
        this.jpa = jpa;
        this.contexto = contexto;
    }

    @Override
    public void guardar(TokenDeRenovacion token) {
        jpa.save(TokenDeRenovacionEntidad.desde(token));
    }

    @Override
    public Optional<TokenDeRenovacion> buscarPorHash(HashDeToken hash) {
        return jpa.findByHash(hash.valor()).map(TokenDeRenovacionEntidad::aDominio);
    }

    @Override
    public boolean marcarComoUsado(UUID id, Instant ahora) {
        return jpa.marcarComoUsado(id, TenantObligatorio.de(contexto), ahora) == 1;
    }
}
