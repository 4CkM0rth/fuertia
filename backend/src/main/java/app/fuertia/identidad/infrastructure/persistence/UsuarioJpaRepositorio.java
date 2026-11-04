package app.fuertia.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de Spring Data. Todos sus métodos heredados (findById, findAll...) pasan por el
 * filtro de tenant de Hibernate. Las únicas excepciones son las consultas nativas de abajo, que
 * están en la lista blanca de ArquitecturaTest.
 */
public interface UsuarioJpaRepositorio extends JpaRepository<UsuarioEntidad, UUID> {

    // Búsqueda GLOBAL a propósito, sin filtro de tenant.
    // Por qué: el correo es con lo que la persona inicia sesión, y en ese momento todavía no sabemos
    // a qué tenant pertenece; por eso tiene que ser único en todo Fuertia (lo garantiza también el
    // índice único uk_usuario_correo). Si lo buscáramos solo dentro del tenant actual, dos negocios
    // podrían registrar el mismo correo y el inicio de sesión sería ambiguo.
    // Es SQL nativo porque Hibernate no aplica el filtro de @TenantId a las consultas nativas, así que
    // funciona igual sin importar en qué contexto de tenant se llame. Solo devuelve sí o no: no expone
    // datos de otro tenant.
    @Query(value = "select exists(select 1 from usuario where correo = :correo)", nativeQuery = true)
    boolean existeCorreoEnTodoElSistema(@Param("correo") String correo);

    // Búsqueda GLOBAL a propósito, para descubrir el tenant al iniciar sesión o pedir recuperación:
    // la persona solo trae su correo y todavía no hay token del cual sacar el tenant. Devuelve
    // únicamente el tenant_id, nada más del usuario; el resto del caso de uso corre ya DENTRO de
    // ese tenant y con el filtro encendido (ver DescubridorDeTenant).
    @Query(value = "select tenant_id from usuario where correo = :correo", nativeQuery = true)
    Optional<UUID> tenantDelCorreo(@Param("correo") String correo);

    /** Consulta derivada (JPQL), así que sí pasa por el filtro de tenant. */
    Optional<UsuarioEntidad> findByCorreo(String correo);
}
