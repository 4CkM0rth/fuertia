package app.fuertia.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenDeRenovacionJpaRepositorio extends JpaRepository<TokenDeRenovacionEntidad, UUID> {

    /** JPQL derivada: pasa por el filtro de tenant. */
    Optional<TokenDeRenovacionEntidad> findByHash(String hash);

    // Ver TokenDeUnUsoJpaRepositorio.marcarComoUsado: la condición "usadoEn is null" resuelve la carrera.
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenDeRenovacionEntidad t set t.usadoEn = :ahora "
            + "where t.id = :id and t.tenantId = :tenant and t.usadoEn is null")
    int marcarComoUsado(@Param("id") UUID id, @Param("tenant") UUID tenant, @Param("ahora") Instant ahora);

    // Búsqueda GLOBAL a propósito, solo para descubrir el tenant: al renovar, el token de acceso ya
    // venció (por eso se renueva) y el único dato es el token de renovación. Devuelve únicamente el
    // tenant_id; el token completo se busca después DENTRO del tenant, con el filtro encendido
    // (ver DescubridorDeTenant).
    @Query(value = "select tenant_id from token_de_renovacion where hash = :hash", nativeQuery = true)
    Optional<UUID> tenantDelToken(@Param("hash") String hash);
}
