package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.UsuarioId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class RepositorioDeTokensDeUnUsoJpa implements RepositorioDeTokensDeUnUso {

    private final TokenDeUnUsoJpaRepositorio jpa;
    private final ContextoDeTenant contexto;

    RepositorioDeTokensDeUnUsoJpa(TokenDeUnUsoJpaRepositorio jpa, ContextoDeTenant contexto) {
        this.jpa = jpa;
        this.contexto = contexto;
    }

    @Override
    public void guardar(TokenDeUnUso token) {
        jpa.save(TokenDeUnUsoEntidad.desde(token));
    }

    @Override
    public Optional<TokenDeUnUso> buscarPorHash(HashDeToken hash) {
        return jpa.findByHash(hash.valor()).map(TokenDeUnUsoEntidad::aDominio);
    }

    @Override
    public boolean marcarComoUsado(UUID id, Instant ahora) {
        return jpa.marcarComoUsado(id, TenantObligatorio.de(contexto), ahora) == 1;
    }

    @Override
    public void invalidarPendientes(UsuarioId usuario, TipoDeTokenDeUnUso tipo, Instant ahora) {
        jpa.invalidarPendientes(usuario.valor(), TenantObligatorio.de(contexto), tipo, ahora);
    }
}
