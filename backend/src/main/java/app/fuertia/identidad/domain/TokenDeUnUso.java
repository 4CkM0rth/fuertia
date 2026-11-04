package app.fuertia.identidad.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Un enlace de un solo uso (verificar el correo o recuperar la contraseña).
 * Guarda el hash del secreto, nunca el secreto.
 */
public record TokenDeUnUso(UUID id, TipoDeTokenDeUnUso tipo, UsuarioId usuarioId, TenantId tenantId,
                           HashDeToken hash, Instant creadoEn, Instant venceEn, Instant usadoEn) {

    public TokenDeUnUso {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(hash);
        Objects.requireNonNull(creadoEn);
        Objects.requireNonNull(venceEn);
        if (!venceEn.isAfter(creadoEn)) {
            throw new IllegalArgumentException("El token debe vencer después de crearse");
        }
    }

    /** Crea el token para un usuario. La vigencia la decide el tipo (24 horas o 1 hora). */
    public static TokenDeUnUso emitir(TipoDeTokenDeUnUso tipo, Usuario usuario, HashDeToken hash, Instant ahora) {
        return new TokenDeUnUso(UUID.randomUUID(), tipo, usuario.id(), usuario.tenantId(), hash, ahora,
                ahora.plus(tipo.vigencia()), null);
    }

    /**
     * Comprueba que se pueda usar ahora. Lanza EnlaceInvalido si ya se usó o venció.
     * Al usuario se le da el mismo mensaje en todos los casos: no le sirve saber cuál fue.
     */
    public void validarUso(TipoDeTokenDeUnUso tipoEsperado, Instant ahora) {
        if (tipo != tipoEsperado || usadoEn != null || !ahora.isBefore(venceEn)) {
            throw new EnlaceInvalido();
        }
    }

    public Optional<Instant> usado() {
        return Optional.ofNullable(usadoEn);
    }
}
