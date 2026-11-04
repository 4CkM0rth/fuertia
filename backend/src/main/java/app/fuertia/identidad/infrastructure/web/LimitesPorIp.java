package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Límites por IP de los endpoints públicos. Los límites por correo los aplican los casos de uso.
 *
 * La IP sale de getRemoteAddr(). En local es la del cliente. En AWS, detrás del balanceador (ALB),
 * getRemoteAddr() sería la del ALB y todos compartirían el mismo contador; por eso allá se activa
 * server.forward-headers-strategy=native (FUERTIA_ENCABEZADOS_PROXY) y Tomcat toma la IP real de
 * X-Forwarded-For, pero SOLO si la petición viene de un proxy de confianza (la red de la VPC,
 * server.tomcat.remoteip.internal-proxies). Si se confiara en cualquiera, un atacante mandaría un
 * X-Forwarded-For inventado distinto en cada intento y nunca llegaría al límite.
 */
@Component
class LimitesPorIp {

    enum Accion {
        REGISTRO(5, Duration.ofHours(1)),
        INICIO_DE_SESION(10, Duration.ofMinutes(15)),
        RECUPERACION(10, Duration.ofHours(1)),
        // Verificar, renovar y confirmar la recuperación usan un token de 256 bits: el límite no es
        // para frenar adivinanzas (imposibles) sino ráfagas que gasten recursos.
        CON_TOKEN(30, Duration.ofMinutes(15));

        private final int maximo;
        private final Duration ventana;

        Accion(int maximo, Duration ventana) {
            this.maximo = maximo;
            this.ventana = ventana;
        }
    }

    private final LimitadorDePeticiones limitador;

    LimitesPorIp(LimitadorDePeticiones limitador) {
        this.limitador = limitador;
    }

    /** Lanza DemasiadosIntentos si esta IP ya se pasó del límite de esa acción. */
    void consumir(HttpServletRequest peticion, Accion accion) {
        limitador.consumir("ip:" + accion.name().toLowerCase() + ":" + peticion.getRemoteAddr(),
                accion.maximo, accion.ventana);
    }
}
