package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;

import java.util.Optional;

/**
 * Puerto de salida: lo que el caso de uso necesita de la persistencia, dicho en el idioma del
 * negocio. El caso de uso no sabe si detrás hay PostgreSQL, una lista en memoria o un servicio.
 */
public interface RepositorioDeUsuarios {

    /**
     * Busca el correo en TODO Fuertia, sin importar el tenant actual.
     * El correo identifica a la persona al iniciar sesión, así que tiene que ser único en el sistema.
     */
    boolean existeCorreo(Correo correo);

    /** Lanza CorreoYaRegistrado si otro registro ganó la carrera y guardó el mismo correo primero. */
    void guardar(Usuario usuario);

    /** Guarda los cambios de un usuario que ya existe (verificado, contraseña). */
    void actualizar(Usuario usuario);

    /** Solo encuentra usuarios del tenant actual. */
    Optional<Usuario> buscarPorId(UsuarioId id);

    /** Solo encuentra usuarios del tenant actual. */
    Optional<Usuario> buscarPorCorreo(Correo correo);
}
