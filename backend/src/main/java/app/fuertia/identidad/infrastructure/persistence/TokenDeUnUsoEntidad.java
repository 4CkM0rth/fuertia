package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.HashDeToken;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
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
@Table(name = "token_de_un_uso")
public class TokenDeUnUsoEntidad implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TipoDeTokenDeUnUso tipo;

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

    protected TokenDeUnUsoEntidad() {
        // Lo exige JPA.
    }

    static TokenDeUnUsoEntidad desde(TokenDeUnUso token) {
        TokenDeUnUsoEntidad entidad = new TokenDeUnUsoEntidad();
        entidad.id = token.id();
        entidad.tenantId = token.tenantId().valor();
        entidad.usuarioId = token.usuarioId().valor();
        entidad.tipo = token.tipo();
        entidad.hash = token.hash().valor();
        entidad.creadoEn = token.creadoEn();
        entidad.venceEn = token.venceEn();
        entidad.usadoEn = token.usadoEn();
        return entidad;
    }

    TokenDeUnUso aDominio() {
        return new TokenDeUnUso(id, tipo, new UsuarioId(usuarioId), new app.fuertia.identidad.domain.TenantId(tenantId),
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
