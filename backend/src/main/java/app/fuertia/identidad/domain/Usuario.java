package app.fuertia.identidad.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Una persona que entra a Fuertia: entrenador, asesorado o administrador.
 * Siempre pertenece a un tenant. Nunca guarda la contraseña en claro, solo su hash.
 *
 * Es inmutable: los cambios (verificar el correo, cambiar la contraseña) devuelven una copia nueva.
 * Así ningún código cambia un usuario "por debajo" sin que quede claro dónde pasó.
 */
public final class Usuario {

    public static final int LONGITUD_MINIMA_NOMBRE = 2;
    public static final int LONGITUD_MAXIMA_NOMBRE = 80;

    private final UsuarioId id;
    private final TenantId tenantId;
    private final String nombre;
    private final Correo correo;
    private final String contrasenaHash;
    private final Rol rol;
    private final String fotoKey;
    private final boolean verificado;
    private final Instant creadoEn;

    public Usuario(UsuarioId id, TenantId tenantId, String nombre, Correo correo, String contrasenaHash,
                   Rol rol, String fotoKey, boolean verificado, Instant creadoEn) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.nombre = validarNombre(nombre);
        this.correo = Objects.requireNonNull(correo);
        if (contrasenaHash == null || contrasenaHash.isBlank()) {
            throw new IllegalArgumentException("El usuario necesita el hash de su contraseña");
        }
        this.contrasenaHash = contrasenaHash;
        this.rol = Objects.requireNonNull(rol);
        this.fotoKey = fotoKey;
        this.verificado = verificado;
        this.creadoEn = Objects.requireNonNull(creadoEn);
    }

    /** El dueño de un negocio recién registrado. Arranca sin verificar y sin foto. */
    public static Usuario nuevoEntrenador(TenantId tenantId, String nombre, Correo correo,
                                          String contrasenaHash, Instant ahora) {
        return new Usuario(UsuarioId.nuevo(), tenantId, nombre, correo, contrasenaHash, Rol.ENTRENADOR, null,
                false, ahora);
    }

    /** Valida el nombre sin crear el usuario. Devuelve el nombre sin espacios a los lados. */
    public static String validarNombre(String nombre) {
        return Textos.requerido(nombre, "nombre", "tu nombre", LONGITUD_MINIMA_NOMBRE, LONGITUD_MAXIMA_NOMBRE);
    }

    public Usuario conCorreoVerificado() {
        return new Usuario(id, tenantId, nombre, correo, contrasenaHash, rol, fotoKey, true, creadoEn);
    }

    public Usuario conContrasena(String nuevoHash) {
        return new Usuario(id, tenantId, nombre, correo, nuevoHash, rol, fotoKey, verificado, creadoEn);
    }

    public UsuarioId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String nombre() {
        return nombre;
    }

    public Correo correo() {
        return correo;
    }

    public String contrasenaHash() {
        return contrasenaHash;
    }

    public Rol rol() {
        return rol;
    }

    /** Clave del objeto en S3. Vacío si el usuario no ha subido foto. */
    public Optional<String> fotoKey() {
        return Optional.ofNullable(fotoKey);
    }

    /** true cuando abrió el enlace del correo (o cambió la contraseña con un enlace, que también lo prueba). */
    public boolean verificado() {
        return verificado;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Usuario usuario && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // Sin el hash: un toString termina en los logs tarde o temprano.
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", tenantId=" + tenantId + ", rol=" + rol + ", verificado=" + verificado + "]";
    }
}
