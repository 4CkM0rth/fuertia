package app.fuertia.identidad.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantIdTest {

    @Test
    void rechazaElIdReservadoDeSinTenant() {
        assertThatThrownBy(() -> new TenantId(new UUID(0L, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void rechazaElIdReservadoDeSistema() {
        assertThatThrownBy(() -> new TenantId(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void rechazaNulo() {
        assertThatThrownBy(() -> new TenantId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aceptaUnIdNormal() {
        UUID valor = UUID.randomUUID();

        assertThat(new TenantId(valor).valor()).isEqualTo(valor);
        assertThat(TenantId.nuevo().valor()).isNotIn(TenantId.RESERVADO_SIN_TENANT, TenantId.RESERVADO_SISTEMA);
    }
}
