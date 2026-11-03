package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.TenantId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba unitaria del contexto y del resolvedor que le entrega el tenant a Hibernate. */
class ContextoDeTenantTest {

    private final ContextoDeTenant contexto = new ContextoDeTenant();
    private final ResolvedorDeTenantActual resolvedor = new ResolvedorDeTenantActual(contexto);

    @Test
    void sinContextoFallaCerradoNoEsRaiz() {
        UUID actual = resolvedor.resolveCurrentTenantIdentifier();

        assertThat(actual).isEqualTo(ResolvedorDeTenantActual.SIN_TENANT);
        assertThat(resolvedor.isRoot(actual)).isFalse();
    }

    @Test
    void dentroDeUnTenantResuelveEseTenant() {
        TenantId tenant = TenantId.nuevo();

        UUID actual = contexto.ejecutarEnTenant(tenant, resolvedor::resolveCurrentTenantIdentifier);

        assertThat(actual).isEqualTo(tenant.valor());
        assertThat(resolvedor.isRoot(actual)).isFalse();
    }

    @Test
    void comoSistemaEsLaRaizQueApagaElFiltro() {
        UUID actual = contexto.ejecutarComoSistema(resolvedor::resolveCurrentTenantIdentifier);

        assertThat(resolvedor.isRoot(actual)).isTrue();
    }

    @Test
    void alSalirRestauraElContextoAnteriorAunqueHayaError() {
        TenantId externo = TenantId.nuevo();

        contexto.ejecutarEnTenant(externo, () -> {
            assertThatThrownBy(() -> contexto.ejecutarEnTenant(TenantId.nuevo(), () -> {
                throw new IllegalStateException("falla adentro");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(contexto.tenantActual()).contains(externo);
            return null;
        });

        assertThat(contexto.tenantActual()).isEmpty();
        assertThat(contexto.esSistema()).isFalse();
    }
}
