package app.fuertia.identidad.infrastructure.web;

import com.icegreen.greenmail.util.GreenMail;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pasos que repiten las pruebas de integración: registrarse, leer el correo, verificar e iniciar
 * sesión. Todo pasa por HTTP como lo haría la web, sin atajos por la base.
 */
final class FlujosDeAcceso {

    static final String CONTRASENA = "una-clave-segura";

    record CorreoRecibido(String asunto, String cuerpo) {
    }

    private final MockMvc mvc;
    private final GreenMail buzon;

    FlujosDeAcceso(MockMvc mvc, GreenMail buzon) {
        this.mvc = mvc;
        this.buzon = buzon;
    }

    /** La base y el buzón se comparten entre pruebas: cada una usa su propio correo para no chocar. */
    static String correoUnico() {
        return "prueba." + UUID.randomUUID() + "@ejemplo.com";
    }

    ResultActions registrar(String nombreNegocio, String nombre, String correo, String contrasena) throws Exception {
        return mvc.perform(post("/api/v1/identidad/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombreNegocio": "%s", "nombre": "%s", "correo": "%s", "contrasena": "%s"}
                """.formatted(nombreNegocio, nombre, correo, contrasena)));
    }

    ResultActions verificar(String token) throws Exception {
        return mvc.perform(post("/api/v1/identidad/verificacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\"}".formatted(token)));
    }

    ResultActions iniciarSesion(String correo, String contrasena, String canal) throws Exception {
        return mvc.perform(post("/api/v1/identidad/sesiones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\", \"contrasena\": \"%s\", \"canal\": \"%s\"}"
                        .formatted(correo, contrasena, canal)));
    }

    ResultActions renovarPorApp(String tokenDeRenovacion) throws Exception {
        return mvc.perform(post("/api/v1/identidad/sesiones/renovacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tokenDeRenovacion\": \"%s\"}".formatted(tokenDeRenovacion)));
    }

    ResultActions solicitarRecuperacion(String correo) throws Exception {
        return mvc.perform(post("/api/v1/identidad/recuperacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\": \"%s\"}".formatted(correo)));
    }

    ResultActions confirmarRecuperacion(String token, String contrasenaNueva) throws Exception {
        return mvc.perform(post("/api/v1/identidad/recuperacion/confirmacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\", \"contrasenaNueva\": \"%s\"}".formatted(token, contrasenaNueva)));
    }

    /** Registra, espera el correo, verifica y devuelve el correo de la cuenta ya lista para entrar. */
    String entrenadorVerificado(String nombreNegocio, String nombre) throws Exception {
        String correo = correoUnico();
        registrar(nombreNegocio, nombre, correo, CONTRASENA).andExpect(status().isAccepted());
        verificar(token(esperarCorreo(correo, 1).getFirst(), "/verificar")).andExpect(status().isNoContent());
        return correo;
    }

    /** Inicia sesión por la app y devuelve el cuerpo de la respuesta. */
    String sesionApp(String correo) throws Exception {
        MvcResult resultado = iniciarSesion(correo, CONTRASENA, "APP").andExpect(status().isOk()).andReturn();
        return resultado.getResponse().getContentAsString();
    }

    static String campo(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    /**
     * Espera hasta que lleguen al menos {@code cuantos} correos para ese destinatario (se envían en
     * segundo plano, después de guardar la transacción) y los devuelve en orden de llegada.
     */
    List<CorreoRecibido> esperarCorreo(String destinatario, int cuantos) {
        await().atMost(Duration.ofSeconds(10)).until(() -> recibidos(destinatario).size() >= cuantos);
        return recibidos(destinatario);
    }

    List<CorreoRecibido> recibidos(String destinatario) {
        return Arrays.stream(buzon.getReceivedMessages())
                .filter(mensaje -> esPara(mensaje, destinatario))
                .map(FlujosDeAcceso::leer)
                .toList();
    }

    /** Saca el token del enlace del correo, por ejemplo /verificar?token=XYZ. */
    static String token(CorreoRecibido correo, String ruta) {
        Matcher m = Pattern.compile(Pattern.quote(ruta) + "\\?token=([A-Za-z0-9_-]{43})").matcher(correo.cuerpo());
        if (!m.find()) {
            throw new AssertionError("El correo no trae el enlace " + ruta + ":\n" + correo.cuerpo());
        }
        return m.group(1);
    }

    private static boolean esPara(MimeMessage mensaje, String destinatario) {
        try {
            return Arrays.stream(mensaje.getRecipients(Message.RecipientType.TO))
                    .anyMatch(direccion -> direccion.toString().equalsIgnoreCase(destinatario));
        } catch (Exception e) {
            return false;
        }
    }

    private static CorreoRecibido leer(MimeMessage mensaje) {
        try {
            // getContent decodifica el texto (quoted-printable, UTF-8): así el enlace sale completo.
            return new CorreoRecibido(mensaje.getSubject(), String.valueOf(mensaje.getContent()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
