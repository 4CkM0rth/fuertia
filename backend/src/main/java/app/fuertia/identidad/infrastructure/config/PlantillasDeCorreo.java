package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.TokenSecreto;

import java.net.URI;

/**
 * Los textos de los correos, en español de Colombia y en texto simple (sin HTML: se ve bien en
 * cualquier cliente de correo y es más difícil de usar para suplantar a Fuertia).
 *
 * Funciones puras: reciben los datos y devuelven el texto. Así se prueban sin enviar nada.
 */
final class PlantillasDeCorreo {

    record Correo(String asunto, String cuerpo) {
    }

    private final String urlWeb;

    PlantillasDeCorreo(URI urlWeb) {
        // Sin "/" al final, para no armar enlaces con "//".
        String texto = urlWeb.toString();
        this.urlWeb = texto.endsWith("/") ? texto.substring(0, texto.length() - 1) : texto;
    }

    Correo verificacion(String nombre, TokenSecreto token) {
        String enlace = urlWeb + "/verificar?token=" + token.valor();
        return new Correo("Confirma tu correo en Fuertia", """
                Hola, %s:

                Gracias por crear tu cuenta en Fuertia. Para empezar, confirma tu correo abriendo este enlace:

                %s

                El enlace sirve una sola vez y vence en 24 horas.

                Si no fuiste tú, ignora este mensaje. Nadie podrá entrar sin confirmar el correo.

                El equipo de Fuertia
                """.formatted(nombre, enlace));
    }

    Correo cuentaExistente() {
        return new Correo("Ya tienes una cuenta en Fuertia", """
                Hola:

                Alguien intentó crear una cuenta en Fuertia con este correo, pero ya tienes una.

                Si fuiste tú, inicia sesión en %s. Si no recuerdas la contraseña, en esa misma página puedes recuperarla.

                Si no fuiste tú, no tienes que hacer nada: tu cuenta sigue igual.

                El equipo de Fuertia
                """.formatted(urlWeb));
    }

    Correo recuperacion(String nombre, TokenSecreto token) {
        String enlace = urlWeb + "/recuperar?token=" + token.valor();
        return new Correo("Cambia tu contraseña de Fuertia", """
                Hola, %s:

                Recibimos una solicitud para cambiar la contraseña de tu cuenta. Para elegir una nueva, abre este enlace:

                %s

                El enlace sirve una sola vez y vence en 1 hora. Al cambiar la contraseña se cierran las sesiones abiertas en todos tus dispositivos.

                Si no fuiste tú, ignora este mensaje: tu contraseña no cambia.

                El equipo de Fuertia
                """.formatted(nombre, enlace));
    }
}
