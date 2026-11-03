package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.PruebaDeIntegracion;
import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Demuestra el filtro global por tenant contra PostgreSQL real: los datos de un negocio no se
 * ven ni se pueden escribir desde el contexto de otro negocio.
 */
@PruebaDeIntegracion
class AislamientoPorTenantIntegracionTest {

    @Autowired
    private RegistrarEntrenador registrarEntrenador;

    @Autowired
    private RepositorioDeUsuarios usuarios;

    @Autowired
    private UsuarioJpaRepositorio usuariosJpa;

    @Autowired
    private ContextoDeTenant contexto;

    private RegistrarEntrenador.Resultado entrenadorA;
    private RegistrarEntrenador.Resultado entrenadorB;

    @BeforeEach
    void registrarDosNegocios() {
        entrenadorA = registrarEntrenador.registrar(comando("Negocio A", "Ana Pérez"));
        entrenadorB = registrarEntrenador.registrar(comando("Negocio B", "Beto Gómez"));
    }

    @Test
    void elUsuarioDelTenantANoApareceConElContextoDelTenantB() {
        // Desde su propio tenant, A se encuentra a sí mismo.
        assertThat(contexto.ejecutarEnTenant(entrenadorA.tenantId(),
                () -> usuarios.buscarPorId(entrenadorA.usuarioId()))).isPresent();

        // Desde el tenant B, el mismo id no existe.
        assertThat(contexto.ejecutarEnTenant(entrenadorB.tenantId(),
                () -> usuarios.buscarPorId(entrenadorA.usuarioId()))).isEmpty();

        // Y un findAll en B solo trae usuarios de B, aunque en la tabla haya de muchos tenants.
        List<UsuarioEntidad> visiblesEnB = contexto.ejecutarEnTenant(entrenadorB.tenantId(), usuariosJpa::findAll);
        assertThat(visiblesEnB)
                .extracting(UsuarioEntidad::getId)
                .contains(entrenadorB.usuarioId().valor())
                .doesNotContain(entrenadorA.usuarioId().valor());
        assertThat(visiblesEnB)
                .extracting(UsuarioEntidad::getTenantId)
                .containsOnly(entrenadorB.tenantId().valor());
    }

    @Test
    void sinContextoDeTenantNoSeVeNingunUsuario() {
        // Falla cerrado: si alguien olvida poner el tenant, no ve nada en vez de verlo todo.
        assertThat(usuarios.buscarPorId(entrenadorA.usuarioId())).isEmpty();
        assertThat(usuariosJpa.findAll()).isEmpty();
    }

    @Test
    void desdeElTenantBNoSePuedeGuardarUnUsuarioEnElTenantA() {
        Usuario intruso = Usuario.nuevoEntrenador(entrenadorA.tenantId(), "Intruso",
                new Correo(correoUnico()), "{argon2}hash-de-prueba", Instant.now());

        // Hibernate rechaza el INSERT: el tenant_id de la fila no es el de la sesión.
        assertThatThrownBy(() -> contexto.ejecutarEnTenant(entrenadorB.tenantId(), () -> {
            usuarios.guardar(intruso);
            return null;
        })).isInstanceOf(RuntimeException.class);

        assertThat(contexto.ejecutarEnTenant(entrenadorA.tenantId(),
                () -> usuarios.buscarPorId(intruso.id()))).isEmpty();
    }

    @Test
    void laBusquedaDeCorreoEsGlobalSinImportarElTenant() {
        // Desde B se detecta que el correo de A ya existe: así el registro no permite repetirlo.
        assertThat(contexto.ejecutarEnTenant(entrenadorB.tenantId(),
                () -> usuarios.existeCorreo(entrenadorA.correo()))).isTrue();
    }

    private static RegistrarEntrenador.Comando comando(String negocio, String nombre) {
        return new RegistrarEntrenador.Comando(negocio, nombre, correoUnico(), "una-clave-segura");
    }

    private static String correoUnico() {
        return "tenant." + UUID.randomUUID() + "@ejemplo.com";
    }
}
