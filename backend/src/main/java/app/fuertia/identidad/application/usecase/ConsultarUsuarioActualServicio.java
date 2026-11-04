package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.ConsultarUsuarioActual;
import app.fuertia.identidad.application.port.in.UsuarioActual;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.SesionInvalida;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;

import java.util.Objects;

public final class ConsultarUsuarioActualServicio implements ConsultarUsuarioActual {

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeTenants tenants;

    public ConsultarUsuarioActualServicio(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.tenants = Objects.requireNonNull(tenants);
    }

    @Override
    public UsuarioActual consultar(UsuarioId id) {
        // buscarPorId filtra por el tenant del token: si el token dijera un tenant que no es el del
        // usuario, no lo encuentra y se responde 401 en vez de mostrar datos cruzados.
        Usuario usuario = usuarios.buscarPorId(id).orElseThrow(SesionInvalida::new);
        Tenant tenant = tenants.buscarPorId(usuario.tenantId()).orElseThrow(SesionInvalida::new);
        return UsuarioActual.de(usuario, tenant);
    }
}
