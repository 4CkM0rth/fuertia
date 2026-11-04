package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.Usuario;
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
@Table(name = "usuario")
public class UsuarioEntidad implements Persistable<UUID> {

    @Id
    private UUID id;

    // @TenantId convierte esta columna en el discriminador del multi-tenant de Hibernate:
    // toda consulta sobre UsuarioEntidad se filtra por el tenant del ResolvedorDeTenantActual,
    // y al insertar Hibernate rechaza un tenant_id distinto al de la sesión.
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = Correo.LONGITUD_MAXIMA)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(name = "foto_key", length = 512)
    private String fotoKey;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // Ver TenantEntidad: el id viene del dominio, así que le decimos a Spring Data cuándo es nueva.
    @Transient
    private boolean nueva = true;

    protected UsuarioEntidad() {
        // Lo exige JPA.
    }

    static UsuarioEntidad desde(Usuario usuario) {
        UsuarioEntidad entidad = new UsuarioEntidad();
        entidad.id = usuario.id().valor();
        entidad.tenantId = usuario.tenantId().valor();
        entidad.nombre = usuario.nombre();
        entidad.correo = usuario.correo().valor();
        entidad.contrasenaHash = usuario.contrasenaHash();
        entidad.rol = usuario.rol();
        entidad.fotoKey = usuario.fotoKey().orElse(null);
        entidad.creadoEn = usuario.creadoEn();
        return entidad;
    }

    Usuario aDominio() {
        // Nombre completo porque la anotación de Hibernate también se llama TenantId.
        return new Usuario(new UsuarioId(id), new app.fuertia.identidad.domain.TenantId(tenantId), nombre,
                new Correo(correo), contrasenaHash, rol, fotoKey, creadoEn);
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
