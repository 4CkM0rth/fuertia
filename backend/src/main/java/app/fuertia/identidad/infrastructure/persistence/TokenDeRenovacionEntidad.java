package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "token_de_renovacion")
public class TokenDeRenovacionEntidad implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Column(nullable = false, length = 64, updatable = false)
    private String hash;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "vence_en", nullable = false, updatable = false)
    private Instant venceEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Transient
    private boolean nueva = true;

    protected TokenDeRenovacionEntidad() {
        // Lo exige JPA.
    }

    static TokenDeRenovacionEntidad desde(TokenDeRenovacion token) {
        TokenDeRenovacionEntidad entidad = new TokenDeRenovacionEntidad();
        entidad.id = token.id();
        entidad.tenantId = token.tenantId().valor();
        entidad.sesionId = token.sesionId().valor();
        entidad.hash = token.hash().valor();
        entidad.creadoEn = token.creadoEn();
        entidad.venceEn = token.venceEn();
        entidad.usadoEn = token.usadoEn();
        return entidad;
    }

    TokenDeRenovacion aDominio() {
        return new TokenDeRenovacion(id, new SesionId(sesionId), new app.fuertia.identidad.domain.TenantId(tenantId),
                new HashDeToken(hash), creadoEn, venceEn, usadoEn);
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
