package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El ejecutor sin Spring ni base: una transacción de mentira y un trabajo que anota en qué tenant
 * corrió y cuántas veces. Cubre el error que hacía fallar 26 pruebas de integración: el trabajo sin
 * resultado corría dos veces, la segunda sin tenant.
 */
class EjecutorDeCasosDeUsoTest {

    /** Transacción que no hace nada pero cuenta cuántas se abrieron. */
    static final class TransaccionesDePrueba extends AbstractPlatformTransactionManager {
        int abiertas;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaccion, TransactionDefinition definicion) {
            abiertas++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus estado) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus estado) {
        }
    }

    private final ContextoDeTenant contexto = new ContextoDeTenant();
    private final TransaccionesDePrueba transacciones = new TransaccionesDePrueba();
    private final EjecutorDeCasosDeUso ejecutor = new EjecutorDeCasosDeUso(contexto, new TransactionTemplate(transacciones));
    private final List<Optional<TenantId>> corridas = new ArrayList<>();

    @Test
    void unTrabajoSinResultadoConTenantCorreUnaSolaVezYDentroDelTenant() {
        TenantId tenant = TenantId.nuevo();

        ejecutor.enTenantSinResultado(Optional.of(tenant), () -> corridas.add(contexto.tenantActual()));

        assertThat(corridas).containsExactly(Optional.of(tenant));
        assertThat(transacciones.abiertas).isEqualTo(1);
        assertThat(contexto.tenantActual()).as("al salir el contexto queda limpio").isEmpty();
    }

    @Test
    void unTrabajoQueDevuelveNullConTenantCorreUnaSolaVez() {
        TenantId tenant = TenantId.nuevo();

        String resultado = ejecutor.enTenant(Optional.of(tenant), () -> {
            corridas.add(contexto.tenantActual());
            return null;
        });

        assertThat(resultado).isNull();
        assertThat(corridas).containsExactly(Optional.of(tenant));
    }

    @Test
    void sinTenantCorreUnaVezSinContexto() {
        ejecutor.enTenantSinResultado(Optional.empty(), () -> corridas.add(contexto.tenantActual()));

        assertThat(corridas).containsExactly(Optional.empty());
        assertThat(transacciones.abiertas).isEqualTo(1);
    }

    @Test
    void conResultadoDevuelveLoQueDevuelveElTrabajo() {
        String resultado = ejecutor.enTenant(Optional.of(TenantId.nuevo()), () -> {
            corridas.add(contexto.tenantActual());
            return "listo";
        });

        assertThat(resultado).isEqualTo("listo");
        assertThat(corridas).hasSize(1);
    }
}
