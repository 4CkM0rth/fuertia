package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RenovarSesion;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeRenovacion;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Sesion;
import app.fuertia.identidad.domain.TokenDeRenovacion;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Rotación del token de renovación: cada token sirve una vez y al usarlo se entrega el siguiente.
 *
 * Detección de robo: si llega un token que YA se usó, alguien tiene una copia (el ladrón o la
 * víctima, no sabemos cuál). Se revoca la sesión completa: toda la familia deja de servir y los
 * dos tienen que volver a iniciar sesión, pero el ladrón no sabe la contraseña.
 */
public final class RenovarSesionServicio implements RenovarSesion {

    private final RepositorioDeTokensDeRenovacion tokens;
    private final RepositorioDeSesiones sesiones;
    private final RepositorioDeUsuarios usuarios;
    private final EmisionDeSesion emision;
    private final Clock reloj;

    public RenovarSesionServicio(RepositorioDeTokensDeRenovacion tokens, RepositorioDeSesiones sesiones,
                                 RepositorioDeUsuarios usuarios, EmisionDeSesion emision, Clock reloj) {
        this.tokens = Objects.requireNonNull(tokens);
        this.sesiones = Objects.requireNonNull(sesiones);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.emision = Objects.requireNonNull(emision);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Resultado renovar(Comando comando) {
        Instant ahora = reloj.instant();
        Optional<TokenDeRenovacion> encontrado = TokenSecreto.leer(comando.tokenDeRenovacion())
                .flatMap(secreto -> tokens.buscarPorHash(secreto.hash()));
        if (encontrado.isEmpty()) {
            return new Rechazada(Motivo.NO_EXISTE);
        }
        TokenDeRenovacion token = encontrado.get();
        Optional<Sesion> sesionEncontrada = sesiones.buscarPorId(token.sesionId());
        if (sesionEncontrada.isEmpty() || !sesionEncontrada.get().estaActiva()) {
            return new Rechazada(Motivo.SESION_CERRADA);
        }
        Sesion sesion = sesionEncontrada.get();

        switch (token.estado(ahora)) {
            case USADO -> {
                sesiones.revocar(sesion.id(), ahora);
                return new Rechazada(Motivo.REUTILIZADO);
            }
            case VENCIDO -> {
                return new Rechazada(Motivo.VENCIDO);
            }
            case VIGENTE -> {
                // sigue abajo
            }
        }

        // Un token de la web tiene que llegar en la cookie y uno de la app en el cuerpo. Si llega
        // por el canal equivocado se rechaza sin gastarlo.
        if (sesion.canal() != comando.canal()) {
            return new Rechazada(Motivo.CANAL_EQUIVOCADO);
        }
        // Si dos peticiones llegan a la vez con el mismo token, solo una gana; la otra cuenta como
        // reutilización.
        if (!tokens.marcarComoUsado(token.id(), ahora)) {
            sesiones.revocar(sesion.id(), ahora);
            return new Rechazada(Motivo.REUTILIZADO);
        }
        Optional<Usuario> usuario = usuarios.buscarPorId(sesion.usuarioId());
        if (usuario.isEmpty()) {
            sesiones.revocar(sesion.id(), ahora);
            return new Rechazada(Motivo.SESION_CERRADA);
        }
        return new Renovada(emision.emitir(usuario.get(), sesion, ahora));
    }
}
