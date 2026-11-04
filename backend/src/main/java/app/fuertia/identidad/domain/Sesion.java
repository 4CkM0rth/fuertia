package app.fuertia.identidad.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Una sesión abierta en un dispositivo. Agrupa a toda la "familia" de tokens de renovación que
 * se van rotando: si uno de ellos se reutiliza, se revoca la sesión completa y la familia entera
 * deja de servir.
 */
public record Sesion(SesionId id, TenantId tenantId, UsuarioId usuarioId, Canal canal, Instant creadaEn,
                     Instant revocadaEn) {

    public Sesion {
        Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(creadaEn);
    }

    public static Sesion abrir(Usuario usuario, Canal canal, Instant ahora) {
        return new Sesion(SesionId.nueva(), usuario.tenantId(), usuario.id(), canal, ahora, null);
    }

    public boolean estaActiva() {
        return revocadaEn == null;
    }

    public Optional<Instant> revocada() {
        return Optional.ofNullable(revocadaEn);
    }
}
