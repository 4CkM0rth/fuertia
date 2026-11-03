package app.fuertia.sistema.infrastructure.web;

import app.fuertia.PruebaDeIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@PruebaDeIntegracion
class EstadoIntegracionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elEstadoEsPublicoYResponde200() throws Exception {
        mvc.perform(get("/api/v1/sistema/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Fuertia"))
                .andExpect(jsonPath("$.version").value("0.1.0"))
                .andExpect(jsonPath("$.estado").value("ok"));
    }

    @Test
    void laSaludDelActuatorEsPublicaYEstaArriba() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unaRutaPrivadaSinAutenticarResponde401() throws Exception {
        mvc.perform(get("/api/v1/identidad/yo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void permiteLlamadasDesdeLaWebLocalPorCors() throws Exception {
        mvc.perform(options("/api/v1/sistema/estado")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
