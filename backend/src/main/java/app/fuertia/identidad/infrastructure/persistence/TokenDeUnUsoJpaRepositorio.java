package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenDeUnUsoJpaRepositorio extends JpaRepository<TokenDeUnUsoEntidad, UUID> {

    /** JPQL derivada: pasa por el filtro de tenant. */
    Optional<TokenDeUnUsoEntidad> findByHash(String hash);

    // Los UPDATE masivos llevan el tenant explícito además del filtro de Hibernate: así no depende
    // de si Hibernate aplica o no el filtro a las sentencias de modificación.
    // "and usadoEn is null" hace que la base decida quién gana si dos peticiones llegan a la vez:
    // solo una cambia la fila (devuelve 1); la otra devuelve 0.
    // flush y clear: guarda antes los cambios pendientes y luego olvida las entidades cargadas, que
    // después de un UPDATE masivo podrían tener datos viejos.
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenDeUnUsoEntidad t set t.usadoEn = :ahora "
            + "where t.id = :id and t.tenantId = :tenant and t.usadoEn is null")
    int marcarComoUsado(@Param("id") UUID id, @Param("tenant") UUID tenant, @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenDeUnUsoEntidad t set t.usadoEn = :ahora "
            + "where t.usuarioId = :usuarioId and t.tenantId = :tenant and t.tipo = :tipo and t.usadoEn is null")
    int invalidarPendientes(@Param("usuarioId") UUID usuarioId, @Param("tenant") UUID tenant,
                            @Param("tipo") TipoDeTokenDeUnUso tipo, @Param("ahora") Instant ahora);

    // Búsqueda GLOBAL a propósito, solo para descubrir el tenant: el enlace del correo llega sin
    // sesión, así que no hay token de acceso del cual sacar el tenant. Devuelve únicamente el
    // tenant_id; la búsqueda del token completo se repite después DENTRO del tenant, con el filtro
    // encendido (ver DescubridorDeTenant). El hash es de 256 bits: nadie lo adivina.
    @Query(value = "select tenant_id from token_de_un_uso where hash = :hash", nativeQuery = true)
    Optional<UUID> tenantDelToken(@Param("hash") String hash);
}
