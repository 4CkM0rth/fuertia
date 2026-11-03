package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envuelve el caso de uso (que es Java puro) con lo que necesita de la infraestructura:
 * una transacción y el contexto de tenant. Implementa el mismo puerto, así el controlador no nota
 * la diferencia. A esto se le llama "decorador".
 *
 * Por qué "como sistema": el registro es la operación que CREA el tenant, así que al empezar no hay
 * un tenant en el cual trabajar. Hibernate fija el tenant al abrir la sesión (al inicio de la
 * transacción), por eso el contexto se pone ANTES de abrir la transacción. En modo sistema
 * Hibernate permite insertar el usuario con el tenant_id del negocio recién creado.
 */
final class RegistrarEntrenadorTransaccional implements RegistrarEntrenador {

    private final RegistrarEntrenador delegado;
    private final ContextoDeTenant contexto;
    private final TransactionTemplate transaccion;

    RegistrarEntrenadorTransaccional(RegistrarEntrenador delegado, ContextoDeTenant contexto,
                                     TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.contexto = contexto;
        this.transaccion = transaccion;
    }

    @Override
    public Resultado registrar(Comando comando) {
        // Si algo falla adentro, la transacción se revierte: no queda un tenant sin su entrenador.
        return contexto.ejecutarComoSistema(() -> transaccion.execute(estado -> delegado.registrar(comando)));
    }
}
