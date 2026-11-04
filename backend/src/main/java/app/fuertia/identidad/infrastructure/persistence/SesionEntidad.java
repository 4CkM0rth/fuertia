package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.UsuarioId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.TenantId;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sesion")
public class SesionEntidad implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private Canal canal;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn;

    @Column(name = "revocada_en")
    private Instant revocadaEn;

    @Transient
    private boolean nueva = true;

    protected SesionEntidad() {
        // Lo exige JPA.
    }

    static SesionEntidad desde(Sesion sesion) {
        SesionEntidad entidad = new SesionEntidad();
        entidad.id = sesion.id().valor();
        entidad.tenantId = sesion.tenantId().valor();
        entidad.usuarioId = sesion.usuarioId().valor();
        entidad.canal = sesion.canal();
        entidad.creadaEn = sesion.creadaEn();
        entidad.revocadaEn = sesion.revocadaEn();
        return entidad;
    }

    Sesion aDominio() {
        return new Sesion(new SesionId(id), new app.fuertia.identidad.domain.TenantId(tenantId), new UsuarioId(usuarioId),
                canal, creadaEn, revocadaEn);
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

    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }
}
