package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;

/** Lo que la web y la app necesitan saber del usuario que inició sesión. */
public record UsuarioActual(UsuarioId id, TenantId tenantId, String nombre, Correo correo, Rol rol,
                            String nombreNegocio) {

    public static UsuarioActual de(Usuario usuario, Tenant tenant) {
        return new UsuarioActual(usuario.id(), usuario.tenantId(), usuario.nombre(), usuario.correo(),
                usuario.rol(), tenant.nombre());
    }
}
