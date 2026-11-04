package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Corre un caso de uso (Java puro) dentro de una transacción y, si hace falta, dentro de un tenant.
 *
 * El orden importa: Hibernate fija el tenant al abrir la sesión, que se abre al empezar la
 * transacción. Por eso primero se pone el tenant en el contexto y DESPUÉS se abre la transacción.
 */
final class EjecutorDeCasosDeUso {

    private final ContextoDeTenant contexto;
    private final TransactionTemplate transaccion;

    EjecutorDeCasosDeUso(ContextoDeTenant contexto, TransactionTemplate transaccion) {
        this.contexto = contexto;
        this.transaccion = transaccion;
    }

    /** Para operaciones con sesión iniciada: el tenant ya lo puso el filtro JWT. */
    <T> T enTransaccion(Supplier<T> trabajo) {
        return transaccion.execute(estado -> trabajo.get());
    }

    void enTransaccionSinResultado(Runnable trabajo) {
        transaccion.executeWithoutResult(estado -> trabajo.run());
    }

    /**
     * Para operaciones públicas: el tenant lo descubrió DescubridorDeTenant a partir del correo o
     * del token. Si no se encontró, corre sin tenant: el caso de uso no verá nada y responderá lo
     * mismo que si el dato no existiera.
     */
    <T> T enTenant(Optional<TenantId> tenant, Supplier<T> trabajo) {
        // Un if explícito y no tenant.map(...).orElseGet(...): Optional.map convierte un resultado
        // null en Optional.empty(), y entonces orElseGet volvía a ejecutar el trabajo, esta vez SIN
        // tenant. Los casos de uso sin resultado (verificar, recuperar) devuelven null y corrían dos
        // veces: la primera funcionaba y la segunda fallaba con EnlaceInvalido.
        if (tenant.isPresent()) {
            return contexto.ejecutarEnTenant(tenant.get(), () -> enTransaccion(trabajo));
        }
        return enTransaccion(trabajo);
    }

    void enTenantSinResultado(Optional<TenantId> tenant, Runnable trabajo) {
        enTenant(tenant, () -> {
            trabajo.run();
            return null;
        });
    }
}
