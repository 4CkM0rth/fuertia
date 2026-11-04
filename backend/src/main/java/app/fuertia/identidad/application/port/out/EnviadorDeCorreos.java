package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.TokenSecreto;

/**
 * Envía los correos del módulo. Los textos y los enlaces los arma el adaptador; el caso de uso
 * solo dice qué correo mandar y a quién.
 *
 * El envío puede ser asíncrono: el adaptador manda el correo después de que se guarde la
 * transacción, así un correo nunca apunta a un token que se deshizo.
 */
public interface EnviadorDeCorreos {

    void enviarEnlaceDeVerificacion(Correo destinatario, String nombre, TokenSecreto token);

    /** Alguien intentó registrarse con un correo que ya tiene cuenta. */
    void enviarAvisoDeCuentaExistente(Correo destinatario);

    void enviarEnlaceDeRecuperacion(Correo destinatario, String nombre, TokenSecreto token);
}
