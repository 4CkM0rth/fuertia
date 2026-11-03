package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.in.RegistrarEntrenador.Comando;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.CorreoInvalido;
import app.fuertia.identidad.domain.CorreoYaRegistrado;
import app.fuertia.identidad.domain.DatoInvalido;
import app.fuertia.identidad.domain.Plan;
import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.Usuario;
import app.fuertia.identidad.domain.UsuarioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba del caso de uso sin Spring ni base de datos. Los puertos de salida se reemplazan por
 * "dobles de prueba": implementaciones mínimas en memoria escritas aquí mismo. Esto es posible
 * justamente porque el caso de uso depende de interfaces (puertos), no de JPA.
 */
class RegistrarEntrenadorServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    private UsuariosEnMemoria usuarios;
    private TenantsEnMemoria tenants;
    private RegistrarEntrenador registrar;

    @BeforeEach
    void preparar() {
        usuarios = new UsuariosEnMemoria();
        tenants = new TenantsEnMemoria();
        registrar = new RegistrarEntrenadorServicio(usuarios, tenants, new CifradorFalso(),
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void creaElTenantYElEntrenador() {
        var resultado = registrar.registrar(comando("Sebastian.Rojas@Ejemplo.com"));

        assertThat(tenants.guardados).singleElement().satisfies(tenant -> {
            assertThat(tenant.id()).isEqualTo(resultado.tenantId());
            assertThat(tenant.nombre()).isEqualTo("Sebastian Rojas Coaching");
            assertThat(tenant.plan()).isEqualTo(Plan.GRATIS);
            assertThat(tenant.creadoEn()).isEqualTo(AHORA);
        });
        assertThat(usuarios.guardados).singleElement().satisfies(usuario -> {
            assertThat(usuario.id()).isEqualTo(resultado.usuarioId());
            assertThat(usuario.tenantId()).isEqualTo(resultado.tenantId());
            assertThat(usuario.rol()).isEqualTo(Rol.ENTRENADOR);
            assertThat(usuario.nombre()).isEqualTo("Sebastian Rojas");
            assertThat(usuario.fotoKey()).isEmpty();
        });
        assertThat(resultado.rol()).isEqualTo(Rol.ENTRENADOR);
        assertThat(resultado.correo().valor()).isEqualTo("sebastian.rojas@ejemplo.com");
    }

    @Test
    void guardaLaContrasenaCifradaNuncaEnClaro() {
        registrar.registrar(comando("ana@correo.com"));

        Usuario guardado = usuarios.guardados.getFirst();
        assertThat(guardado.contrasenaHash()).isEqualTo("cifrada(una-clave-segura)");
    }

    @Test
    void rechazaUnCorreoQueYaExisteAunqueCambienLasMayusculas() {
        registrar.registrar(comando("ana@correo.com"));

        assertThatThrownBy(() -> registrar.registrar(comando("ANA@Correo.com")))
                .isInstanceOf(CorreoYaRegistrado.class)
                .hasMessage("Ya existe una cuenta con ese correo");
        assertThat(tenants.guardados).hasSize(1);
        assertThat(usuarios.guardados).hasSize(1);
    }

    @Test
    void rechazaUnCorreoInvalidoSinGuardarNada() {
        assertThatThrownBy(() -> registrar.registrar(comando("no-es-correo")))
                .isInstanceOf(CorreoInvalido.class);
        assertThat(tenants.guardados).isEmpty();
        assertThat(usuarios.guardados).isEmpty();
    }

    @Test
    void rechazaUnaContrasenaCorta() {
        var comando = new Comando("Negocio", "Ana", "ana@correo.com", "corta");

        assertThatThrownBy(() -> registrar.registrar(comando))
                .isInstanceOfSatisfying(DatoInvalido.class, error -> assertThat(error.campo()).isEqualTo("contrasena"));
        assertThat(usuarios.guardados).isEmpty();
    }

    @Test
    void elComandoNoMuestraLaContrasenaEnSuToString() {
        assertThat(comando("ana@correo.com").toString()).doesNotContain("una-clave-segura");
    }

    private static Comando comando(String correo) {
        return new Comando("Sebastian Rojas Coaching", "Sebastian Rojas", correo, "una-clave-segura");
    }

    // --- Dobles de prueba de los puertos de salida ---

    private static final class UsuariosEnMemoria implements RepositorioDeUsuarios {
        final List<Usuario> guardados = new ArrayList<>();

        @Override
        public boolean existeCorreo(Correo correo) {
            return guardados.stream().anyMatch(usuario -> usuario.correo().equals(correo));
        }

        @Override
        public void guardar(Usuario usuario) {
            guardados.add(usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(UsuarioId id) {
            return guardados.stream().filter(usuario -> usuario.id().equals(id)).findFirst();
        }
    }

    private static final class TenantsEnMemoria implements RepositorioDeTenants {
        final List<Tenant> guardados = new ArrayList<>();

        @Override
        public void guardar(Tenant tenant) {
            guardados.add(tenant);
        }

        @Override
        public Optional<Tenant> buscarPorId(TenantId id) {
            return guardados.stream().filter(tenant -> tenant.id().equals(id)).findFirst();
        }
    }

    // Cifrado de mentiras, predecible, para poder comprobar que el caso de uso lo usó.
    private static final class CifradorFalso implements CifradorDeContrasenas {
        @Override
        public String cifrar(String contrasenaEnClaro) {
            return "cifrada(" + contrasenaEnClaro + ")";
        }

        @Override
        public boolean coincide(String contrasenaEnClaro, String hash) {
            return cifrar(contrasenaEnClaro).equals(hash);
        }
    }
}
