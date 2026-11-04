package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.TenantId;

import java.util.UUID;

/**
 * El tenant actual para los UPDATE masivos. Si no hay uno, es un error de programación (alguien
 * llamó sin pasar por el filtro JWT ni por DescubridorDeTenant) y se falla en voz alta en vez de
 * modificar filas sin saber de quién son.
 */
final class TenantObligatorio {

    private TenantObligatorio() {
    }

    static UUID de(ContextoDeTenant contexto) {
        return contexto.tenantActual()
                .map(TenantId::valor)
                .orElseThrow(() -> new IllegalStateException("Esta operación necesita un tenant en el contexto"));
    }
}
