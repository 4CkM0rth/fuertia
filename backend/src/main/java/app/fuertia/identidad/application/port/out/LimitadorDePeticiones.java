package app.fuertia.identidad.application.port.out;

import java.time.Duration;

/**
 * Cuenta intentos por clave (una IP, un correo) dentro de una ventana de tiempo.
 * Hoy está en Redis, para que todas las copias del backend compartan el mismo contador.
 */
public interface LimitadorDePeticiones {

    /**
     * Cuenta un intento. Si con este se pasa del máximo en la ventana, lanza DemasiadosIntentos
     * con el tiempo que falta para que la ventana se reinicie.
     */
    void consumir(String clave, int maximo, Duration ventana);
}
