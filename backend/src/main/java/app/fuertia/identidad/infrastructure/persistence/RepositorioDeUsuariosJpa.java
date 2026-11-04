package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.CorreoYaRegistrado;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Adaptador: implementa el puerto RepositorioDeUsuarios con JPA. */
@Component
class RepositorioDeUsuariosJpa implements RepositorioDeUsuarios {

    // Nombre del índice único definido en V1__identidad.sql.
    private static final String RESTRICCION_CORREO_UNICO = "uk_usuario_correo";

    private final UsuarioJpaRepositorio jpa;

    RepositorioDeUsuariosJpa(UsuarioJpaRepositorio jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existeCorreo(Correo correo) {
        return jpa.existeCorreoEnTodoElSistema(correo.valor());
    }

    @Override
    public void guardar(Usuario usuario) {
        try {
            // saveAndFlush envía el INSERT ya, para enterarnos aquí (y no al confirmar la
            // transacción) si la base rechazó el correo repetido.
            jpa.saveAndFlush(UsuarioEntidad.desde(usuario));
        } catch (DataIntegrityViolationException e) {
            // Dos registros con el mismo correo al mismo tiempo: los dos pasan existeCorreo, pero
            // la base solo deja entrar uno. Al otro le respondemos igual que si ya existiera.
            if (violoRestriccion(e, RESTRICCION_CORREO_UNICO)) {
                throw new CorreoYaRegistrado(usuario.correo());
            }
            throw e;
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(UsuarioId id) {
        return jpa.findById(id.valor()).map(UsuarioEntidad::aDominio);
    }

    private static boolean violoRestriccion(Throwable error, String restriccion) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion
                    && restriccion.equalsIgnoreCase(violacion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
