package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador: implementa el puerto RepositorioDeTenants con JPA. */
@Component
class RepositorioDeTenantsJpa implements RepositorioDeTenants {

    private final TenantJpaRepositorio jpa;

    RepositorioDeTenantsJpa(TenantJpaRepositorio jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(Tenant tenant) {
        jpa.save(TenantEntidad.desde(tenant));
    }

    @Override
    public Optional<Tenant> buscarPorId(TenantId id) {
        return jpa.findById(id.valor()).map(TenantEntidad::aDominio);
    }
}
