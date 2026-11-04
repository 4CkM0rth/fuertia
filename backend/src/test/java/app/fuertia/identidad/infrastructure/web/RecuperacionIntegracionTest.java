package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.CONTRASENA;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.campo;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.correoUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PruebaDeIntegracion
class RecuperacionIntegracionTest {

    private static final String NUEVA = "clave-nueva-segura";
    private static final String MENSAJE =
            "Si hay una cuenta con ese correo, te enviamos un enlace para cambiar la contraseña.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GreenMail buzon;

    private FlujosDeAcceso flujos;
    private String correo;

    @BeforeEach
    void entrenadorListo() throws Exception {
        flujos = new FlujosDeAcceso(mvc, buzon);
        correo = flujos.entrenadorVerificado("Negocio", "Ana Pérez");
    }

    @Test
    void recuperacionCompletaCambiaLaClaveYCierraLasSesionesAbiertas() throws Exception {
        String sesionVieja = flujos.sesionApp(correo);

        flujos.solicitarRecuperacion(correo)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE));
        // El primer correo fue el de verificación; el segundo, el de recuperación.
        var recibido = flujos.esperarCorreo(correo, 2).get(1);
        assertThat(recibido.asunto()).isEqualTo("Cambia tu contraseña de Fuertia");
        String token = FlujosDeAcceso.token(recibido, "/recuperar");

        flujos.confirmarRecuperacion(token, NUEVA).andExpect(status().isNoContent());

        flujos.iniciarSesion(correo, NUEVA, "APP").andExpect(status().isOk());
        flujos.iniciarSesion(correo, CONTRASENA, "APP").andExpect(status().isUnauthorized());
        // La sesión que estaba abierta antes del cambio ya no se puede renovar.
        flujos.renovarPorApp(campo(sesionVieja, "$.tokenDeRenovacion")).andExpect(status().isUnauthorized());
    }

    @Test
    void conCorreoInexistenteRespondeLoMismoYNoEnviaNada() throws Exception {
        String inexistente = correoUnico();

        flujos.solicitarRecuperacion(inexistente)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE));

        // Se da tiempo al envío en segundo plano antes de afirmar que no llegó nada.
        await().pollDelay(Duration.ofMillis(500)).atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(flujos.recibidos(inexistente)).isEmpty());
    }

    @Test
    void elEnlaceNoSirveDosVeces() throws Exception {
        flujos.solicitarRecuperacion(correo).andExpect(status().isAccepted());
        String token = FlujosDeAcceso.token(flujos.esperarCorreo(correo, 2).get(1), "/recuperar");
        flujos.confirmarRecuperacion(token, NUEVA).andExpect(status().isNoContent());

        flujos.confirmarRecuperacion(token, "otra-clave-nueva")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"));
    }

    @Test
    void alCuartoPedidoEnUnaHoraParaElMismoCorreoResponde429() throws Exception {
        for (int i = 0; i < 3; i++) {
            flujos.solicitarRecuperacion(correo).andExpect(status().isAccepted());
        }

        flujos.solicitarRecuperacion(correo)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
