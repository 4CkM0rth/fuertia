package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.CONTRASENA;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.correoUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/v1/identidad/registro de punta a punta: HTTP, seguridad, caso de uso, PostgreSQL y correo. */
@PruebaDeIntegracion
class RegistroIntegracionTest {

    private static final String MENSAJE = "{\"mensaje\":\"Te enviamos un correo. Ábrelo para continuar.\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GreenMail buzon;

    @Autowired
    private JdbcTemplate jdbc;

    private FlujosDeAcceso flujos;

    @BeforeEach
    void preparar() {
        flujos = new FlujosDeAcceso(mvc, buzon);
    }

    @Test
    void conCorreoNuevoResponde202CreaLaCuentaSinVerificarYEnviaElEnlace() throws Exception {
        String correo = "Sebastian." + java.util.UUID.randomUUID() + "@Ejemplo.com";
        String normalizado = correo.toLowerCase(Locale.ROOT);

        flujos.registrar("Sebastian Rojas Coaching", "Sebastian Rojas", correo, CONTRASENA)
                .andExpect(status().isAccepted())
                .andExpect(content().json(MENSAJE, true));

        Boolean verificado = jdbc.queryForObject("select verificado from usuario where correo = ?", Boolean.class, normalizado);
        assertThat(verificado).isFalse();
        var correos = flujos.esperarCorreo(normalizado, 1);
        assertThat(correos).singleElement().satisfies(recibido -> {
            assertThat(recibido.asunto()).isEqualTo("Confirma tu correo en Fuertia");
            assertThat(recibido.cuerpo()).contains("Hola, Sebastian Rojas")
                    .contains("http://localhost:5173/verificar?token=");
        });
        // En la base solo está el hash del token, nunca el token del correo.
        String token = FlujosDeAcceso.token(correos.getFirst(), "/verificar");
        Integer conElTokenEnClaro = jdbc.queryForObject("select count(*) from token_de_un_uso where hash = ?",
                Integer.class, token);
        assertThat(conElTokenEnClaro).isZero();
    }

    @Test
    void conCorreoRepetidoRespondeLoMismoNoCreaNadaYEnviaElAviso() throws Exception {
        String correo = correoUnico();
        String primera = flujos.registrar("Negocio uno", "Ana Pérez", correo, CONTRASENA)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        flujos.esperarCorreo(correo, 1);

        String segunda = flujos.registrar("Negocio dos", "Otra Ana", correo.toUpperCase(Locale.ROOT), "otra-clave-segura")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        // Desde afuera las dos respuestas son idénticas: no se puede saber si el correo existía.
        assertThat(segunda).isEqualTo(primera);
        Integer tenantsDelSegundo = jdbc.queryForObject("select count(*) from tenant where nombre = 'Negocio dos'", Integer.class);
        assertThat(tenantsDelSegundo).isZero();
        var correos = flujos.esperarCorreo(correo, 2);
        assertThat(correos).extracting(FlujosDeAcceso.CorreoRecibido::asunto)
                .containsExactly("Confirma tu correo en Fuertia", "Ya tienes una cuenta en Fuertia");
        assertThat(correos.get(1).cuerpo()).doesNotContain("token=");
    }

    @Test
    void guardaLaContrasenaCifradaConArgon2() throws Exception {
        String correo = correoUnico();

        flujos.registrar("Negocio", "Ana Pérez", correo, CONTRASENA).andExpect(status().isAccepted());

        String hash = jdbc.queryForObject("select contrasena_hash from usuario where correo = ?", String.class, correo);
        assertThat(hash).startsWith("{argon2}").doesNotContain(CONTRASENA);
    }

    @Test
    void datosInvalidosResponden400ConErroresEnEspanol() throws Exception {
        flujos.registrar("", "A", "no-es-correo", "corta")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.nombreNegocio").value("Escribe el nombre de tu negocio"))
                .andExpect(jsonPath("$.errores.nombre").value("Tu nombre debe tener entre 2 y 80 caracteres"))
                .andExpect(jsonPath("$.errores.correo").value("Escribe un correo válido"))
                .andExpect(jsonPath("$.errores.contrasena").value("La contraseña debe tener entre 10 y 128 caracteres"));
    }

    @Test
    void correoSinDominioRealResponde400DesdeElDominio() throws Exception {
        // @Email del formulario acepta "ana@localhost"; el objeto de valor Correo no.
        flujos.registrar("Negocio", "Ana Pérez", "ana@localhost", CONTRASENA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.correo").value("Escribe un correo válido"));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mvc.perform(post("/api/v1/identidad/registro").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos inválidos"));
    }

    @Test
    void alSextoRegistroDesdeLaMismaIpEnUnaHoraResponde429ConRetryAfter() throws Exception {
        for (int i = 0; i < 5; i++) {
            flujos.registrar("Negocio " + i, "Ana Pérez", correoUnico(), CONTRASENA).andExpect(status().isAccepted());
        }

        flujos.registrar("Negocio 6", "Ana Pérez", correoUnico(), CONTRASENA)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Hiciste demasiados intentos. Espera un momento y vuelve a probar."));
    }
}
