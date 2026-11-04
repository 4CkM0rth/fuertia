package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.Canal;

public interface IniciarSesion {

    /**
     * Lanza CredencialesInvalidas (correo inexistente o clave mala, sin distinguir),
     * CorreoSinVerificar (solo si la clave es correcta) o DemasiadosIntentos.
     */
    SesionIniciada iniciar(Comando comando);

    record Comando(String correo, String contrasena, Canal canal) {

        @Override
        public String toString() {
            return "Comando[correo=" + correo + ", contrasena=****, canal=" + canal + "]";
        }
    }
}
