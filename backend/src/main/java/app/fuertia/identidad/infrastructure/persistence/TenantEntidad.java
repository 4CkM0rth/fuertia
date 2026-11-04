package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.Plan;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla tenant. Es la raíz del multi-tenant y por eso NO lleva tenant_id ni @TenantId:
 * el tenant no pertenece a otro tenant.
 *
 * Es una clase distinta de domain.Tenant a propósito: las anotaciones de JPA se quedan aquí y el
 * dominio sigue siendo Java puro.
 */
@Entity
@Table(name = "tenant")
public class TenantEntidad implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // El id lo genera el dominio, no la base. Sin esto Spring Data vería un id lleno, creería que
    // la fila ya existe y haría un SELECT de más antes de insertar.
    @Transient
    private boolean nueva = true;

    protected TenantEntidad() {
        // Lo exige JPA.
    }

    static TenantEntidad desde(Tenant tenant) {
        TenantEntidad entidad = new TenantEntidad();
        entidad.id = tenant.id().valor();
        entidad.nombre = tenant.nombre();
        entidad.plan = tenant.plan();
        entidad.creadoEn = tenant.creadoEn();
        return entidad;
    }

    Tenant aDominio() {
        return new Tenant(new TenantId(id), nombre, plan, creadoEn);
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        nueva = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }
}
