package app.fuertia.identidad.infrastructure.persistence;

import app.fuertia.PruebaDeIntegracion;
import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.Usuario;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Demuestra el filtro global por tenant contra PostgreSQL real: los datos de un negocio no se
 * ven, no se borran y no se pisan desde el contexto de otro negocio.
 */
@PruebaDeIntegracion
class AislamientoPorTenantIntegracionTest {

    // Mensaje de Hibernate (TenantIdGeneration) cuando el tenant_id de la fila no es el de la sesión.
    private static final String TENANT_DISTINTO = "assigned tenant id differs from current tenant id [%s != %s]";

    @Autowired
    private RegistrarEntrenador registrarEntrenador;

    @Autowired
    private RepositorioDeUsuarios usuarios;

    @Autowired
    private UsuarioJpaRepositorio usuariosJpa;

    @Autowired
    private ContextoDeTenant contexto;

    @Autowired
    private JdbcTemplate jdbc;

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
        assertThat(enTenant(entrenadorA.tenantId(), () -> usuarios.buscarPorId(entrenadorA.usuarioId()))).isPresent();

        // Desde el tenant B, el mismo id no existe.
        assertThat(enTenant(entrenadorB.tenantId(), () -> usuarios.buscarPorId(entrenadorA.usuarioId()))).isEmpty();

        // Y un findAll en B solo trae usuarios de B, aunque en la tabla haya de muchos tenants.
        List<UsuarioEntidad> visiblesEnB = enTenant(entrenadorB.tenantId(), usuariosJpa::findAll);
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
    void desdeElTenantBNoSePuedeInsertarUnUsuarioEnElTenantA() {
        Usuario intruso = usuarioNuevoEn(entrenadorA.tenantId());

        assertThatThrownBy(() -> enTenant(entrenadorB.tenantId(), () -> guardar(intruso)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining(TENANT_DISTINTO.formatted(entrenadorA.tenantId(), entrenadorB.tenantId()));

        assertThat(enTenant(entrenadorA.tenantId(), () -> usuarios.buscarPorId(intruso.id()))).isEmpty();
    }

    @Test
    void sinContextoNoSePuedeInsertarUnUsuario() {
        Usuario huerfano = usuarioNuevoEn(entrenadorA.tenantId());

        // Sin contexto la sesión queda en el tenant centinela "sin tenant", que no coincide con A.
        assertThatThrownBy(() -> usuarios.guardar(huerfano))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining(TENANT_DISTINTO.formatted(entrenadorA.tenantId(), TenantId.RESERVADO_SIN_TENANT));

        assertThat(enTenant(entrenadorA.tenantId(), () -> usuarios.buscarPorId(huerfano.id()))).isEmpty();
    }

    @Test
    void desdeElTenantBNoSePuedeBorrarUnUsuarioDelTenantA() {
        // deleteById primero busca la fila (con el filtro de B), no la encuentra y no hace nada.
        enTenant(entrenadorB.tenantId(), () -> {
            usuariosJpa.deleteById(entrenadorA.usuarioId().valor());
            return null;
        });

        assertThat(enTenant(entrenadorA.tenantId(), () -> usuarios.buscarPorId(entrenadorA.usuarioId()))).isPresent();
    }

    @Test
    void desdeElTenantBNoSePuedePisarUnUsuarioDelTenantAReusandoSuId() {
        // B arma un usuario con el id de A pero con su propio tenant, para intentar sobrescribirlo.
        Usuario suplantador = new Usuario(entrenadorA.usuarioId(), entrenadorB.tenantId(), "Suplantador",
                new Correo(correoUnico()), "{argon2}hash-de-prueba", Rol.ADMIN, null, Instant.now());

        // B no puede ver la fila de A, así que el INSERT choca con la llave primaria: no hay UPDATE posible.
        assertThatThrownBy(() -> enTenant(entrenadorB.tenantId(), () -> guardar(suplantador)))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, error ->
                        assertThat(nombreDeRestriccionViolada(error)).isEqualTo("usuario_pkey"));

        Usuario original = enTenant(entrenadorA.tenantId(), () -> usuarios.buscarPorId(entrenadorA.usuarioId()))
                .orElseThrow();
        assertThat(original.nombre()).isEqualTo("Ana Pérez");
        assertThat(original.rol()).isEqualTo(Rol.ENTRENADOR);
        assertThat(original.tenantId()).isEqualTo(entrenadorA.tenantId());
    }

    @Test
    void laBusquedaDeCorreoEsGlobalSinImportarElTenant() {
        // Desde B se detecta que el correo de A ya existe: así el registro no permite repetirlo.
        assertThat(enTenant(entrenadorB.tenantId(), () -> usuarios.existeCorreo(entrenadorA.correo()))).isTrue();
    }

    @Test
    void laBaseRechazaLosIdsDeTenantReservados() {
        // V2__restricciones_tenant.sql: aunque alguien escriba por fuera de la app.
        for (UUID reservado : List.of(TenantId.RESERVADO_SIN_TENANT, TenantId.RESERVADO_SISTEMA)) {
            assertThatThrownBy(() -> jdbc.update("insert into tenant (id, nombre) values (?, 'Reservado')", reservado))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_tenant_id_no_reservado");
        }
    }

    private <T> T enTenant(TenantId tenant, java.util.function.Supplier<T> trabajo) {
        return contexto.ejecutarEnTenant(tenant, trabajo);
    }

    private Void guardar(Usuario usuario) {
        usuarios.guardar(usuario);
        return null;
    }

    private static String nombreDeRestriccionViolada(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                return violacion.getConstraintName();
            }
        }
        return null;
    }

    private static Usuario usuarioNuevoEn(TenantId tenant) {
        return Usuario.nuevoEntrenador(tenant, "Intruso", new Correo(correoUnico()), "{argon2}hash-de-prueba",
                Instant.now());
    }

    private static RegistrarEntrenador.Comando comando(String negocio, String nombre) {
        return new RegistrarEntrenador.Comando(negocio, nombre, correoUnico(), "una-clave-segura");
    }

    private static String correoUnico() {
        return "tenant." + UUID.randomUUID() + "@ejemplo.com";
    }
}
