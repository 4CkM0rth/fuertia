package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.UsuarioId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
class RepositorioDeSesionesJpa implements RepositorioDeSesiones {

    private final SesionJpaRepositorio jpa;
    private final ContextoDeTenant contexto;

    RepositorioDeSesionesJpa(SesionJpaRepositorio jpa, ContextoDeTenant contexto) {
        this.jpa = jpa;
        this.contexto = contexto;
    }

    @Override
    public void guardar(Sesion sesion) {
        jpa.save(SesionEntidad.desde(sesion));
    }

    @Override
    public Optional<Sesion> buscarPorId(SesionId id) {
        return jpa.findById(id.valor()).map(SesionEntidad::aDominio);
    }

    @Override
    public void revocar(SesionId id, Instant ahora) {
        jpa.revocar(id.valor(), TenantObligatorio.de(contexto), ahora);
    }

    @Override
    public void revocarTodasDe(UsuarioId usuario, Instant ahora) {
        jpa.revocarTodasDe(usuario.valor(), TenantObligatorio.de(contexto), ahora);
    }
}
