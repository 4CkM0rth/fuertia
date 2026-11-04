package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.TokenSecreto;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradorYCorreosTest {

    @Test
    void argon2GuardaConPrefijoYComparaBien() {
        CifradorArgon2 cifrador = new CifradorArgon2(2, Duration.ofSeconds(5));

        String hash = cifrador.cifrar("una-clave-segura");

        assertThat(hash).startsWith("{argon2}$argon2id$").contains("m=19456,t=2,p=1").doesNotContain("una-clave-segura");
        assertThat(cifrador.coincide("una-clave-segura", hash)).isTrue();
        assertThat(cifrador.coincide("otra-clave", hash)).isFalse();
    }

    @Test
    void elSenueloUsaLosMismosParametrosYNingunaClaveLoAcierta() {
        CifradorArgon2 cifrador = new CifradorArgon2(2, Duration.ofSeconds(5));

        assertThat(cifrador.hashSenuelo()).startsWith("{argon2}$argon2id$").contains("m=19456,t=2,p=1");
        assertThat(cifrador.coincide("", cifrador.hashSenuelo())).isFalse();
        assertThat(cifrador.coincide("una-clave-segura", cifrador.hashSenuelo())).isFalse();
    }

    @Test
    void siNoHayTurnoATiempoRespondeOcupado() throws Exception {
        // Un solo turno y 1 ms de espera: un cálculo de Argon2 con 19 MiB tarda mucho más que eso,
        // así que si 8 hilos piden a la vez, al menos 7 se quedan sin turno.
        CifradorArgon2 cifrador = new CifradorArgon2(1, Duration.ofMillis(1));
        String hash = new CifradorArgon2(1, Duration.ofSeconds(5)).cifrar("una-clave-segura");
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger sinTurno = new AtomicInteger();
        Thread[] hilos = new Thread[8];
        for (int i = 0; i < hilos.length; i++) {
            hilos[i] = new Thread(() -> {
                try {
                    salida.await();
                    cifrador.coincide("una-clave-segura", hash);
                } catch (CifradorOcupado e) {
                    sinTurno.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            hilos[i].start();
        }

        salida.countDown(); // todos arrancan a la vez
        for (Thread hilo : hilos) {
            hilo.join(TimeUnit.SECONDS.toMillis(30));
        }

        assertThat(sinTurno.get()).isGreaterThanOrEqualTo(1);
        // Y después de la ráfaga el turno quedó libre otra vez.
        assertThat(cifrador.coincide("una-clave-segura", hash)).isTrue();
    }

    @Test
    void lasPlantillasArmanLosEnlacesYNoUsanGuionesLargos() {
        PlantillasDeCorreo plantillas = new PlantillasDeCorreo(URI.create("http://localhost:5173/"));
        TokenSecreto token = TokenSecreto.generar();

        var verificacion = plantillas.verificacion("Ana", token);
        var recuperacion = plantillas.recuperacion("Ana", token);
        var existente = plantillas.cuentaExistente();

        assertThat(verificacion.cuerpo()).contains("http://localhost:5173/verificar?token=" + token.valor())
                .contains("24 horas").contains("Hola, Ana");
        assertThat(recuperacion.cuerpo()).contains("http://localhost:5173/recuperar?token=" + token.valor())
                .contains("1 hora");
        assertThat(existente.cuerpo()).contains("ya tienes una").contains("http://localhost:5173");
        for (var correo : new PlantillasDeCorreo.Correo[] {verificacion, recuperacion, existente}) {
            assertThat(correo.asunto() + correo.cuerpo()).doesNotContain("—").doesNotContain("//verificar");
            // El token nunca en el asunto, que muchos clientes muestran en notificaciones.
            assertThat(correo.asunto()).doesNotContain(token.valor());
        }
    }

    @Test
    void conUnaLlaveInvalidaElMensajeNoMuestraLaLlave() {
        String basura = "-----BEGIN PRIVATE KEY-----\nAAAAsecretoAAAA\n-----END PRIVATE KEY-----";

        assertThatThrownBy(() -> LlavesDeJwt.cargar(basura, true))
                .isInstanceOf(IllegalStateException.class)
                .message().doesNotContain("secreto");
    }
}
