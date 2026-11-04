package app.fuertia.identidad.application.port.in;

public interface ConfirmarRecuperacion {

    /** Cambia la contraseña y cierra todas las sesiones. Lanza EnlaceInvalido o DatoInvalido. */
    void confirmar(Comando comando);

    record Comando(String token, String contrasenaNueva) {

        @Override
        public String toString() {
            return "Comando[token=****, contrasenaNueva=****]";
        }
    }
}
