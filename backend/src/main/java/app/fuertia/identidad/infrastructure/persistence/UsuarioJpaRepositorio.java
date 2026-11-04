package app.fuertia.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repositorio de Spring Data. Todos sus métodos heredados (findById, findAll...) pasan por el
 * filtro de tenant de Hibernate. La única excepción es la consulta nativa de abajo.
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
}
