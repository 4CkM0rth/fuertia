package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TenantId;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Responde una sola pregunta: "¿a qué tenant pertenece este correo o este token?".
 *
 * Hace falta en las operaciones públicas (iniciar sesión, renovar, abrir un enlace), donde todavía
 * no hay un token de acceso del cual sacar el tenant. En vez de apagar el filtro por tenant para
 * toda la operación, solo esta consulta es global y devuelve solo el tenant_id. Después la
 * infraestructura abre la transacción DENTRO de ese tenant y el caso de uso trabaja con el filtro
 * encendido, como cualquier otro.
 *
 * Si no encuentra nada, la operación corre sin tenant y el caso de uso tampoco encuentra nada,
 * así que responde el mismo error que si el dato no existiera.
 */
@Component
public class DescubridorDeTenant {

    private final UsuarioJpaRepositorio usuarios;
    private final TokenDeUnUsoJpaRepositorio tokensDeUnUso;
    private final TokenDeRenovacionJpaRepositorio tokensDeRenovacion;

    DescubridorDeTenant(UsuarioJpaRepositorio usuarios, TokenDeUnUsoJpaRepositorio tokensDeUnUso,
                        TokenDeRenovacionJpaRepositorio tokensDeRenovacion) {
        this.usuarios = usuarios;
        this.tokensDeUnUso = tokensDeUnUso;
        this.tokensDeRenovacion = tokensDeRenovacion;
    }

    public Optional<TenantId> porCorreo(Correo correo) {
        return usuarios.tenantDelCorreo(correo.valor()).map(DescubridorDeTenant::aTenant);
    }

    public Optional<TenantId> porTokenDeUnUso(HashDeToken hash) {
        return tokensDeUnUso.tenantDelToken(hash.valor()).map(DescubridorDeTenant::aTenant);
    }

    public Optional<TenantId> porTokenDeRenovacion(HashDeToken hash) {
        return tokensDeRenovacion.tenantDelToken(hash.valor()).map(DescubridorDeTenant::aTenant);
    }

    private static TenantId aTenant(UUID valor) {
        return new TenantId(valor);
    }
}
