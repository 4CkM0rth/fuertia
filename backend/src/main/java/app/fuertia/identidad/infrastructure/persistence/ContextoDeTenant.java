package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.TenantId;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Recuerda, para el hilo que atiende la petición actual, en nombre de qué tenant se trabaja.
 * Hibernate lo consulta (a través de ResolvedorDeTenantActual) cada vez que abre una sesión,
 * y con eso filtra todas las consultas por tenant_id.
 *
 * Hay tres situaciones posibles:
 * - En un tenant: lo normal. Solo se ven y se escriben datos de ese tenant.
 * - Como sistema: el filtro se apaga. Solo para operaciones que por naturaleza no tienen
 *   tenant todavía, como el registro, que es justamente el que lo crea.
 * - Sin contexto: no se ve nada. Si alguien olvida poner el tenant, falla cerrado en vez de abierto.
 *
 * Usa ThreadLocal porque cada petición HTTP se atiende en su propio hilo. Siempre se entra con
 * ejecutarEn... y se restaura el valor anterior al salir, así no queda "pegado" a un hilo reutilizado.
 */
public final class ContextoDeTenant {

    private sealed interface Alcance permits EnTenant, ComoSistema {
    }

    private record EnTenant(TenantId tenantId) implements Alcance {
    }

    private record ComoSistema() implements Alcance {
    }

    private final ThreadLocal<Alcance> alcance = new ThreadLocal<>();

    public <T> T ejecutarEnTenant(TenantId tenantId, Supplier<T> trabajo) {
        Objects.requireNonNull(tenantId, "El tenant es obligatorio");
        return ejecutar(new EnTenant(tenantId), trabajo);
    }

    public <T> T ejecutarComoSistema(Supplier<T> trabajo) {
        return ejecutar(new ComoSistema(), trabajo);
    }

    /**
     * Igual que ejecutarEnTenant, pero para usar con try-with-resources (que es un try/finally):
     *
     *   try (var alcance = contexto.entrarEnTenant(tenant)) { cadena.doFilter(peticion, respuesta); }
     *
     * Existe porque el filtro HTTP necesita envolver código que lanza IOException y ServletException,
     * y un Supplier no las deja pasar. Es peligroso si se olvida cerrar (el tenant quedaría pegado al
     * hilo y lo heredaría la siguiente petición), por eso ArquitecturaTest solo deja usarlo al filtro.
     */
    public AlcanceAbierto entrarEnTenant(TenantId tenantId) {
        Objects.requireNonNull(tenantId, "El tenant es obligatorio");
        Alcance anterior = alcance.get();
        alcance.set(new EnTenant(tenantId));
        return () -> restaurar(anterior);
    }

    /** Lo que devuelve entrarEnTenant. Al cerrarlo se restaura el contexto que había antes. */
    @FunctionalInterface
    public interface AlcanceAbierto extends AutoCloseable {
        @Override
        void close();
    }

    public Optional<TenantId> tenantActual() {
        return alcance.get() instanceof EnTenant(TenantId tenantId) ? Optional.of(tenantId) : Optional.empty();
    }

    public boolean esSistema() {
        return alcance.get() instanceof ComoSistema;
    }

    private <T> T ejecutar(Alcance nuevo, Supplier<T> trabajo) {
        Alcance anterior = alcance.get();
        alcance.set(nuevo);
        try {
            return trabajo.get();
        } finally {
            restaurar(anterior);
        }
    }

    private void restaurar(Alcance anterior) {
        if (anterior == null) {
            alcance.remove();
        } else {
            alcance.set(anterior);
        }
    }
}
