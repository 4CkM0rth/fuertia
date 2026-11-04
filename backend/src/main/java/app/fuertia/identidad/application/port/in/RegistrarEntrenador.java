package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.UsuarioId;

/**
 * Puerto de entrada: lo que el mundo exterior (la API REST, una prueba, un script) puede pedirle
 * al módulo de identidad. Quien llama solo conoce esta interfaz, no la clase que la implementa.
 */
public interface RegistrarEntrenador {

    Resultado registrar(Comando comando);

    /** Lo que llega para registrar un entrenador. La contraseña viene en claro y se cifra en el caso de uso. */
    record Comando(String nombreNegocio, String nombre, String correo, String contrasena) {

        // Un record imprime todos sus campos por defecto; la contraseña no puede terminar en un log.
        @Override
        public String toString() {
            return "Comando[nombreNegocio=" + nombreNegocio + ", nombre=" + nombre + ", correo=" + correo
                    + ", contrasena=****]";
        }
    }

    /**
     * Qué pasó por dentro. La API NO lo muestra: responde 202 igual en los dos casos para no
     * revelar si el correo ya tiene cuenta. Sirve para las pruebas y para quien llame desde código.
     */
    sealed interface Resultado permits CuentaCreada, CuentaExistente {
    }

    record CuentaCreada(TenantId tenantId, UsuarioId usuarioId, Correo correo) implements Resultado {
    }

    record CuentaExistente(Correo correo) implements Resultado {
    }
}
