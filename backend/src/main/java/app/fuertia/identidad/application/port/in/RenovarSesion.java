package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.domain.Canal;

public interface RenovarSesion {

    /**
     * Devuelve un resultado en vez de lanzar una excepción cuando rechaza el token. Motivo: si el
     * token se reutilizó, la sesión se revoca y ese cambio TIENE que guardarse; una excepción dentro
     * de la transacción la desharía.
     */
    Resultado renovar(Comando comando);

    /** canal: por dónde llegó el token (cookie = WEB, cuerpo = APP). Debe coincidir con el de la sesión. */
    record Comando(String tokenDeRenovacion, Canal canal) {

        @Override
        public String toString() {
            return "Comando[tokenDeRenovacion=****, canal=" + canal + "]";
        }
    }

    sealed interface Resultado permits Renovada, Rechazada {
    }

    record Renovada(SesionIniciada sesion) implements Resultado {
    }

    record Rechazada(Motivo motivo) implements Resultado {
    }

    /** Solo para los logs y las pruebas. Al cliente siempre se le responde el mismo 401. */
    enum Motivo {
        NO_EXISTE,
        VENCIDO,
        SESION_CERRADA,
        CANAL_EQUIVOCADO,
        /** Se presentó un token ya usado: se revocó la sesión completa. */
        REUTILIZADO
    }
}
