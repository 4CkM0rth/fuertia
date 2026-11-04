package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.out.EnviadorDeCorreos;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.TokenSecreto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.RejectedExecutionException;

/**
 * Adaptador del puerto EnviadorDeCorreos por SMTP. Local habla con Mailpit; en AWS, con SES por
 * SMTP. Solo cambian FUERTIA_SMTP_HOST, FUERTIA_SMTP_PUERTO y el usuario y la clave.
 *
 * Envía fuera de la petición, en un hilo aparte, y solo después de que la transacción se guarde:
 * - La respuesta no espera al servidor de correo (que puede tardar segundos), y el registro con un
 *   correo nuevo tarda lo mismo que con uno repetido.
 * - Si la transacción se deshace, el correo no sale: nunca llega un enlace a un token que no existe.
 *
 * Nunca escribe en el log el token, el enlace ni el destinatario.
 */
final class EnviadorDeCorreosSmtp implements EnviadorDeCorreos {

    private static final Logger log = LoggerFactory.getLogger(EnviadorDeCorreosSmtp.class);

    private final JavaMailSender servidor;
    private final TaskExecutor ejecutor;
    private final PlantillasDeCorreo plantillas;
    private final String remitente;

    EnviadorDeCorreosSmtp(JavaMailSender servidor, TaskExecutor ejecutor, PlantillasDeCorreo plantillas,
                          String remitente) {
        this.servidor = servidor;
        this.ejecutor = ejecutor;
        this.plantillas = plantillas;
        this.remitente = remitente;
    }

    @Override
    public void enviarEnlaceDeVerificacion(Correo destinatario, String nombre, TokenSecreto token) {
        enviarDespuesDeGuardar("verificacion", destinatario, plantillas.verificacion(nombre, token));
    }

    @Override
    public void enviarAvisoDeCuentaExistente(Correo destinatario) {
        enviarDespuesDeGuardar("cuenta-existente", destinatario, plantillas.cuentaExistente());
    }

    @Override
    public void enviarEnlaceDeRecuperacion(Correo destinatario, String nombre, TokenSecreto token) {
        enviarDespuesDeGuardar("recuperacion", destinatario, plantillas.recuperacion(nombre, token));
    }

    private void enviarDespuesDeGuardar(String tipo, Correo destinatario, PlantillasDeCorreo.Correo contenido) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario.valor());
        mensaje.setSubject(contenido.asunto());
        mensaje.setText(contenido.cuerpo());

        Runnable envio = () -> enviar(tipo, mensaje);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    encolar(tipo, envio);
                }
            });
        } else {
            encolar(tipo, envio);
        }
    }

    /** Spring lo llama al apagar la app: espera los correos que quedaban en la cola. */
    public void close() {
        if (ejecutor instanceof ThreadPoolTaskExecutor hilos) {
            hilos.shutdown();
        }
    }

    private void encolar(String tipo, Runnable envio) {
        try {
            ejecutor.execute(envio);
        } catch (RejectedExecutionException e) {
            log.error("Cola de correos llena: no se envió un correo de tipo {}", tipo);
        }
    }

    private void enviar(String tipo, SimpleMailMessage mensaje) {
        try {
            servidor.send(mensaje);
        } catch (MailException e) {
            // Solo el tipo y la clase del error: el mensaje de la excepción puede traer el destinatario.
            log.error("No se pudo enviar un correo de tipo {} ({})", tipo, e.getClass().getSimpleName());
        }
    }
}
