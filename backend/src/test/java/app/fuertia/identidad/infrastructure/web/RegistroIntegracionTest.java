package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/identidad/registro de punta a punta: HTTP, seguridad, caso de uso, JPA y PostgreSQL real. */
@PruebaDeIntegracion
class RegistroIntegracionTest {

    private static final String URL = "/api/v1/identidad/registro";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registraUnEntrenadorYResponde201() throws Exception {
        String correo = "Sebastian." + UUID.randomUUID() + "@Ejemplo.com";

        registrar("Sebastian Rojas Coaching", "Sebastian Rojas", correo, "una-clave-segura")
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantId").isNotEmpty())
                .andExpect(jsonPath("$.usuarioId").isNotEmpty())
                .andExpect(jsonPath("$.correo").value(correo.toLowerCase(Locale.ROOT)))
                .andExpect(jsonPath("$.rol").value("ENTRENADOR"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void guardaLaContrasenaCifradaConArgon2() throws Exception {
        String correo = correoUnico();

        registrar("Negocio", "Ana Pérez", correo, "una-clave-segura").andExpect(status().isCreated());

        String hash = jdbc.queryForObject("select contrasena_hash from usuario where correo = ?", String.class, correo);
        assertThat(hash).startsWith("{argon2}").doesNotContain("una-clave-segura");
    }

    @Test
    void correoDuplicadoResponde409AunqueCambienLasMayusculas() throws Exception {
        String correo = correoUnico();
        registrar("Negocio uno", "Ana Pérez", correo, "una-clave-segura").andExpect(status().isCreated());

        registrar("Negocio dos", "Otra Ana", correo.toUpperCase(Locale.ROOT), "otra-clave-segura")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Ya existe una cuenta con ese correo"));

        Integer tenantsDelSegundoIntento = jdbc.queryForObject(
                "select count(*) from tenant where nombre = 'Negocio dos'", Integer.class);
        assertThat(tenantsDelSegundoIntento).as("el tenant del intento fallido no debe quedar guardado").isZero();
    }

    @Test
    void datosInvalidosResponden400ConErroresEnEspanol() throws Exception {
        registrar("", "A", "no-es-correo", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombreNegocio").value("Escribe el nombre de tu negocio"))
                .andExpect(jsonPath("$.errores.nombre").value("Tu nombre debe tener entre 2 y 80 caracteres"))
                .andExpect(jsonPath("$.errores.correo").value("Escribe un correo válido"))
                .andExpect(jsonPath("$.errores.contrasena").value("La contraseña debe tener entre 10 y 128 caracteres"));
    }

    @Test
    void correoSinDominioRealResponde400DesdeElDominio() throws Exception {
        // @Email del formulario acepta "ana@localhost"; el objeto de valor Correo no.
        registrar("Negocio", "Ana Pérez", "ana@localhost", "una-clave-segura")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").value("Escribe un correo válido"));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"));
    }

    private ResultActions registrar(String nombreNegocio, String nombre, String correo, String contrasena)
            throws Exception {
        String cuerpo = """
                {"nombreNegocio": "%s", "nombre": "%s", "correo": "%s", "contrasena": "%s"}
                """.formatted(nombreNegocio, nombre, correo, contrasena);
        return mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // La base se comparte entre pruebas: cada una usa su propio correo para no chocar.
    private static String correoUnico() {
        return "prueba." + UUID.randomUUID() + "@ejemplo.com";
    }
}
