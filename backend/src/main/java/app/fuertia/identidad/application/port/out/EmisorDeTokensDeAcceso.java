package app.fuertia.identidad.application.port.out;

import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.Usuario;

import java.time.Instant;

/** Emite el token de acceso de corta duración. Hoy es un JWT firmado con ES256. */
public interface EmisorDeTokensDeAcceso {

    TokenDeAcceso emitir(Usuario usuario, SesionId sesion, Instant ahora);

    record TokenDeAcceso(String valor, Instant venceEn) {

        @Override
        public String toString() {
            return "TokenDeAcceso[****, venceEn=" + venceEn + "]";
        }
    }
}
