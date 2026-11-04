package app.fuertia.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Los UPDATE masivos llevan el tenant explícito además del filtro de Hibernate: así no dependen de
 * si Hibernate aplica o no el filtro a las sentencias de modificación.
 * @Transactional: un UPDATE masivo exige transacción. Si ya hay una (la del caso de uso), se une a
 * ella; si no, abre la suya.
 */
public interface SesionJpaRepositorio extends JpaRepository<SesionEntidad, UUID> {

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SesionEntidad s set s.revocadaEn = :ahora "
            + "where s.id = :id and s.tenantId = :tenant and s.revocadaEn is null")
    int revocar(@Param("id") UUID id, @Param("tenant") UUID tenant, @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SesionEntidad s set s.revocadaEn = :ahora "
            + "where s.usuarioId = :usuarioId and s.tenantId = :tenant and s.revocadaEn is null")
    int revocarTodasDe(@Param("usuarioId") UUID usuarioId, @Param("tenant") UUID tenant, @Param("ahora") Instant ahora);
}
