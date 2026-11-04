package app.fuertia.identidad.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import com.icegreen.greenmail.util.GreenMail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.CONTRASENA;
import static app.fuertia.identidad.infrastructure.web.FlujosDeAcceso.correoUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PruebaDeIntegracion
class VerificacionIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private GreenMail buzon;

    @Autowired
    private JdbcTemplate jdbc;

    private FlujosDeAcceso flujos;
    private String correo;
    private String token;

    @BeforeEach
    void registrar() throws Exception {
        flujos = new FlujosDeAcceso(mvc, buzon);
        correo = correoUnico();
        flujos.registrar("Negocio", "Ana Pérez", correo, CONTRASENA).andExpect(status().isAccepted());
        token = FlujosDeAcceso.token(flujos.esperarCorreo(correo, 1).getFirst(), "/verificar");
    }

    @Test
    void conElEnlaceValidoResponde204YLaCuentaQuedaVerificada() throws Exception {
        flujos.verificar(token).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select verificado from usuario where correo = ?", Boolean.class, correo)).isTrue();
    }

    @Test
    void elMismoEnlaceUsadoDosVecesResponde400EnlaceInvalido() throws Exception {
        flujos.verificar(token).andExpect(status().isNoContent());

        enlaceInvalido(flujos.verificar(token));
    }

    @Test
    void unEnlaceVencidoResponde400EnlaceInvalidoYNoVerifica() throws Exception {
        jdbc.update("""
                update token_de_un_uso set creado_en = now() - interval '2 days', vence_en = now() - interval '1 day'
                where usuario_id = (select id from usuario where correo = ?)
                """, correo);

        enlaceInvalido(flujos.verificar(token));
        assertThat(jdbc.queryForObject("select verificado from usuario where correo = ?", Boolean.class, correo)).isFalse();
    }

    @Test
    void unTokenInventadoResponde400EnlaceInvalido() throws Exception {
        enlaceInvalido(flujos.verificar("A".repeat(43)));
        enlaceInvalido(flujos.verificar("corto"));
    }

    private static void enlaceInvalido(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"))
                .andExpect(jsonPath("$.title").value("El enlace no es válido o ya venció"));
    }
}
