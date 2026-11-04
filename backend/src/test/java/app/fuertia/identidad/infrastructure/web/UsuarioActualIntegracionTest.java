package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import app.fuertia.identidad.infrastructure.config.FabricaDeJwt;
import app.fuertia.identidad.infrastructure.config.LlavesDeJwt;
import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.campo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /yo con dos negocios, y el filtro JWT: el tenant y el rol salen solo del token, y un token
 * falso, vencido o para otra API no entra.
 */
@PruebaDeIntegracion
class UsuarioActualIntegracionTest {

    private static final String YO = "/api/v1/identidad/yo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GreenMail buzon;

    /** El mismo codificador (y la misma llave) que usa la app: sirve para armar tokens a la medida. */
    @Autowired
    private JwtEncoder codificador;

    private String sesionA;
    private String sesionB;

    @BeforeEach
    void dosNegocios() throws Exception {
        FlujosDeAcceso flujos = new FlujosDeAcceso(mvc, buzon);
        sesionA = flujos.sesionApp(flujos.entrenadorVerificado("Negocio A", "Ana Pérez"));
        sesionB = flujos.sesionApp(flujos.entrenadorVerificado("Negocio B", "Beto Gómez"));
    }

    @Test
    void cadaEntrenadorVeSoloSusDatos() throws Exception {
        yoCon(campo(sesionA, "$.tokenDeAcceso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campo(sesionA, "$.usuario.id")))
                .andExpect(jsonPath("$.tenantId").value(campo(sesionA, "$.usuario.tenantId")))
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.nombreNegocio").value("Negocio A"))
                .andExpect(jsonPath("$.rol").value("ENTRENADOR"));

        yoCon(campo(sesionB, "$.tokenDeAcceso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Beto Gómez"))
                .andExpect(jsonPath("$.nombreNegocio").value("Negocio B"));
    }

    @Test
    void unTokenBienFirmadoConElUsuarioDeAYElTenantDeBNoVeNada() throws Exception {
        // Aunque alguien lograra un token con un tid ajeno, el filtro busca el usuario DENTRO de ese
        // tenant, no lo encuentra y responde 401 en vez de mostrar datos cruzados.
        String cruzado = token(c -> c
                .subject(campo(sesionA, "$.usuario.id"))
                .claim("tid", campo(sesionB, "$.usuario.tenantId")));

        yoCon(cruzado)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Tu sesión no es válida. Inicia sesión otra vez."));
    }

    @Test
    void sinTokenResponde401EnProblemDetails() throws Exception {
        mvc.perform(get(YO))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Necesitas iniciar sesión"));
    }

    @Test
    void unTokenConFirmaInvalidaSeRechaza() throws Exception {
        JwtEncoder otraLlave = FabricaDeJwt.codificador(LlavesDeJwt.temporales());
        String falso = otraLlave.encode(JwtEncoderParameters.from(encabezado(), claimsDeA().build())).getTokenValue();

        yoCon(falso).andExpect(status().isUnauthorized());
        // Y un token real con un caracter cambiado en la firma tampoco.
        String real = campo(sesionA, "$.tokenDeAcceso");
        String alterado = real.substring(0, real.length() - 2) + (real.endsWith("AA") ? "BB" : "AA");
        yoCon(alterado).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenVencidoSeRechaza() throws Exception {
        String vencido = token(c -> c
                .issuedAt(Instant.now().minus(Duration.ofMinutes(30)))
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5))));

        yoCon(vencido).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenParaOtraApiSeRechaza() throws Exception {
        yoCon(token(c -> c.audience(List.of("otra-api")))).andExpect(status().isUnauthorized());
    }

    @Test
    void unAsesoradoNoPuedeUsarUnEndpointDeEntrenador() throws Exception {
        mvc.perform(get(SoloEntrenadorDePruebaController.RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(c -> c.claim("rol", "ASESORADO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("No tienes permiso para hacer esto"));

        mvc.perform(get(SoloEntrenadorDePruebaController.RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + campo(sesionA, "$.tokenDeAcceso")))
                .andExpect(status().isOk());
    }

    private ResultActions yoCon(String tokenDeAcceso) throws Exception {
        return mvc.perform(get(YO).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeAcceso));
    }

    /** Un token como los de la app, con los datos de A, que cada prueba modifica a su gusto. */
    private JwtClaimsSet.Builder claimsDeA() {
        Instant ahora = Instant.now();
        return JwtClaimsSet.builder()
                .issuer("fuertia")
                .audience(List.of("fuertia-api"))
                .subject(campo(sesionA, "$.usuario.id"))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .claim("tid", campo(sesionA, "$.usuario.tenantId"))
                .claim("rol", "ENTRENADOR")
                .claim("sid", UUID.randomUUID().toString());
    }

    private String token(Consumer<JwtClaimsSet.Builder> cambio) {
        JwtClaimsSet.Builder claims = claimsDeA();
        cambio.accept(claims);
        return codificador.encode(JwtEncoderParameters.from(encabezado(), claims.build())).getTokenValue();
    }

    private static JwsHeader encabezado() {
        return JwsHeader.with(SignatureAlgorithm.ES256).type("at+jwt").build();
    }
}
