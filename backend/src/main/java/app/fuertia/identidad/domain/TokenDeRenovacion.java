package app.fuertia.identidad.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un eslabón de la familia de tokens de renovación de una sesión. Sirve una sola vez: al usarlo
 * se emite el siguiente. Guarda el hash del secreto, nunca el secreto.
 */
public record TokenDeRenovacion(UUID id, SesionId sesionId, TenantId tenantId, HashDeToken hash,
                                Instant creadoEn, Instant venceEn, Instant usadoEn) {

    public static final Duration VIGENCIA = Duration.ofDays(30);

    public enum Estado {
        VIGENTE,
        /** Ya se cambió por otro. Si vuelve a llegar, alguien tiene una copia: señal de robo. */
        USADO,
        VENCIDO
    }

    public TokenDeRenovacion {
        Objects.requireNonNull(id);
        Objects.requireNonNull(sesionId);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(hash);
        Objects.requireNonNull(creadoEn);
        Objects.requireNonNull(venceEn);
    }

    public static TokenDeRenovacion emitir(Sesion sesion, HashDeToken hash, Instant ahora) {
        return new TokenDeRenovacion(UUID.randomUUID(), sesion.id(), sesion.tenantId(), hash, ahora,
                ahora.plus(VIGENCIA), null);
    }

    // Se revisa primero si ya se usó: un token reutilizado es señal de robo aunque además esté vencido.
    public Estado estado(Instant ahora) {
        if (usadoEn != null) {
            return Estado.USADO;
        }
        return ahora.isBefore(venceEn) ? Estado.VIGENTE : Estado.VENCIDO;
    }
}
