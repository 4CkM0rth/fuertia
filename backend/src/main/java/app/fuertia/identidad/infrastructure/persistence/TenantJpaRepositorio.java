package app.fuertia.identidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio de Spring Data. Solo lo usa el adaptador; el resto del sistema habla con el puerto. */
public interface TenantJpaRepositorio extends JpaRepository<TenantEntidad, UUID> {
}
