package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;

import java.util.Optional;

public interface RepositorioDeTenants {

    void guardar(Tenant tenant);

    Optional<Tenant> buscarPorId(TenantId id);
}
