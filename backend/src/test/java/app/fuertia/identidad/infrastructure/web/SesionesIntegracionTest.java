package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import com.icegreen.greenmail.util.GreenMail;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.CONTRASENA;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.campo;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.correoUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inicio de sesión, renovación con rotación, cookie con CSRF en la web y cierre de sesión. */
@PruebaDeIntegracion
class SesionesIntegracionTest {

    private static final String RENOVACION = "/api/v1/identidad/sesiones/renovacion";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GreenMail buzon;

    private FlujosDeAcceso flujos;
    private String correo;

    @BeforeEach
    void entrenadorListo() throws Exception {
        flujos = new FlujosDeAcceso(mvc, buzon);
        correo = flujos.entrenadorVerificado("Negocio de Ana", "Ana Pérez");
    }

    // --- Inicio de sesión ---

    @Test
    void conCanalAppDevuelveElTokenDeAccesoYElDeRenovacionEnElCuerpo() throws Exception {
        flujos.iniciarSesion(correo, CONTRASENA, "APP")
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.tokenDeAcceso").isNotEmpty())
                .andExpect(jsonPath("$.tokenDeRenovacion").isNotEmpty())
                .andExpect(jsonPath("$.venceEnSegundos").value(org.hamcrest.Matchers.both(
                        org.hamcrest.Matchers.greaterThan(890)).and(org.hamcrest.Matchers.lessThanOrEqualTo(900))))
                .andExpect(jsonPath("$.usuario.correo").value(correo))
                .andExpect(jsonPath("$.usuario.rol").value("ENTRENADOR"))
                .andExpect(jsonPath("$.usuario.nombreNegocio").value("Negocio de Ana"));
    }

    @Test
    void conCanalWebElTokenDeRenovacionVaEnUnaCookieProtegidaYNoEnElCuerpo() throws Exception {
        MvcResult resultado = flujos.iniciarSesion(correo, CONTRASENA, "WEB")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenDeAcceso").isNotEmpty())
                .andExpect(jsonPath("$.tokenDeRenovacion").doesNotExist())
                .andReturn();

        String cookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("fuertia_renovacion=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/identidad/sesiones", "Max-Age=2592000");
        assertThat(valorDeCookie(cookie)).hasSize(43);
    }

    @Test
    void conClaveMalaOCorreoInexistenteLaRespuestaEsLaMisma() throws Exception {
        String claveMala = flujos.iniciarSesion(correo, "otra-clave-cualquiera", "WEB")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Correo o contraseña incorrectos"))
                .andReturn().getResponse().getContentAsString();

        String correoInexistente = flujos.iniciarSesion(correoUnico(), "otra-clave-cualquiera", "WEB")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(correoInexistente).isEqualTo(claveMala);
    }

    @Test
    void sinVerificarYConClaveCorrectaResponde403CorreoSinVerificar() throws Exception {
        String sinVerificar = correoUnico();
        flujos.registrar("Negocio", "Beto Gómez", sinVerificar, CONTRASENA).andExpect(status().isAccepted());

        flujos.iniciarSesion(sinVerificar, CONTRASENA, "WEB")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CORREO_SIN_VERIFICAR"))
                .andExpect(jsonPath("$.title").value("Confirma tu correo para entrar"));

        // Con la clave mala no se revela que la cuenta existe y está sin verificar.
        flujos.iniciarSesion(sinVerificar, "otra-clave-cualquiera", "WEB").andExpect(status().isUnauthorized());
    }

    @Test
    void alSextoIntentoConElMismoCorreoResponde429() throws Exception {
        for (int i = 0; i < 5; i++) {
            flujos.iniciarSesion(correo, "otra-clave-cualquiera", "APP").andExpect(status().isUnauthorized());
        }

        flujos.iniciarSesion(correo, CONTRASENA, "APP")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    // --- Renovación por la app ---

    @Test
    void renovarEntregaUnTokenNuevoYElNuevoTambienSirve() throws Exception {
        String primero = campo(flujos.sesionApp(correo), "$.tokenDeRenovacion");

        String respuesta = flujos.renovarPorApp(primero).andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenDeAcceso").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String segundo = campo(respuesta, "$.tokenDeRenovacion");

        assertThat(segundo).isNotEqualTo(primero);
        flujos.renovarPorApp(segundo).andExpect(status().isOk());
    }

    @Test
    void reutilizarUnTokenCierraLaSesionCompleta() throws Exception {
        String primero = campo(flujos.sesionApp(correo), "$.tokenDeRenovacion");
        String segundo = campo(flujos.renovarPorApp(primero).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.tokenDeRenovacion");

        // Alguien usa otra vez el primero: es una copia robada o una repetición. Se cierra todo.
        flujos.renovarPorApp(primero)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Tu sesión no es válida. Inicia sesión otra vez."));

        // El más nuevo de la familia también dejó de servir.
        flujos.renovarPorApp(segundo).andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenOConUnoInventadoResponde401() throws Exception {
        mvc.perform(post(RENOVACION)).andExpect(status().isUnauthorized());
        flujos.renovarPorApp("A".repeat(43)).andExpect(status().isUnauthorized());
    }

    // --- Renovación por la web: cookie y CSRF ---

    @Test
    void conLaCookieYElEncabezadoCsrfRenuevaYCambiaLaCookie() throws Exception {
        String primera = cookieDeInicioWeb();

        MvcResult resultado = renovarPorWeb(primera, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenDeRenovacion").doesNotExist())
                .andReturn();

        String nueva = valorDeCookie(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(nueva).hasSize(43).isNotEqualTo(primera);
        renovarPorWeb(nueva, "1").andExpect(status().isOk());
    }

    @Test
    void conLaCookiePeroSinElEncabezadoCsrfResponde401SinGastarElToken() throws Exception {
        String cookie = cookieDeInicioWeb();

        renovarPorWeb(cookie, null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
        renovarPorWeb(cookie, "otro-valor").andExpect(status().isUnauthorized());

        // El token no se tocó: con el encabezado correcto sigue sirviendo.
        renovarPorWeb(cookie, "1").andExpect(status().isOk());
    }

    @Test
    void unTokenDeLaWebNoSirveEnElCuerpoComoSiFueraDeLaApp() throws Exception {
        String cookie = cookieDeInicioWeb();

        flujos.renovarPorApp(cookie).andExpect(status().isUnauthorized());
        renovarPorWeb(cookie, "1").andExpect(status().isOk());
    }

    // --- Cierre de sesión ---

    @Test
    void cerrarSesionRevocaLaSesionYBorraLaCookie() throws Exception {
        String sesion = flujos.sesionApp(correo);

        mvc.perform(delete("/api/v1/identidad/sesiones/actual")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + campo(sesion, "$.tokenDeAcceso")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("fuertia_renovacion="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"))));

        flujos.renovarPorApp(campo(sesion, "$.tokenDeRenovacion")).andExpect(status().isUnauthorized());
    }

    @Test
    void cerrarSesionSinTokenResponde401() throws Exception {
        mvc.perform(delete("/api/v1/identidad/sesiones/actual")).andExpect(status().isUnauthorized());
    }

    private String cookieDeInicioWeb() throws Exception {
        MvcResult resultado = flujos.iniciarSesion(correo, CONTRASENA, "WEB").andExpect(status().isOk()).andReturn();
        return valorDeCookie(resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private ResultActions renovarPorWeb(String cookie, String csrf) throws Exception {
        var peticion = post(RENOVACION).cookie(new Cookie("fuertia_renovacion", cookie));
        if (csrf != null) {
            peticion.header("X-Fuertia-Csrf", csrf);
        }
        return mvc.perform(peticion);
    }

    private static String valorDeCookie(String setCookie) {
        Matcher m = Pattern.compile("fuertia_renovacion=([^;]*)").matcher(setCookie);
        assertThat(m.find()).as("Set-Cookie: " + setCookie).isTrue();
        return m.group(1);
    }
}
