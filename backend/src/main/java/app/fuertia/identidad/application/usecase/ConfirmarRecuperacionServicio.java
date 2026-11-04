package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.ConfirmarRecuperacion;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.EnlaceInvalido;
import app.fuertia.identidad.domain.PoliticaDeContrasena;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class ConfirmarRecuperacionServicio implements ConfirmarRecuperacion {

    private final RepositorioDeTokensDeUnUso tokens;
    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeSesiones sesiones;
    private final CifradorDeContrasenas cifrador;
    private final Clock reloj;

    public ConfirmarRecuperacionServicio(RepositorioDeTokensDeUnUso tokens, RepositorioDeUsuarios usuarios,
                                         RepositorioDeSesiones sesiones, CifradorDeContrasenas cifrador, Clock reloj) {
        this.tokens = Objects.requireNonNull(tokens);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.sesiones = Objects.requireNonNull(sesiones);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void confirmar(Comando comando) {
        // La regla de la contraseña va primero: no gasta el enlace si la clave nueva no sirve.
        PoliticaDeContrasena.validar(comando.contrasenaNueva());
        Instant ahora = reloj.instant();
        TokenSecreto secreto = TokenSecreto.leer(comando.token()).orElseThrow(EnlaceInvalido::new);
        TokenDeUnUso token = tokens.buscarPorHash(secreto.hash()).orElseThrow(EnlaceInvalido::new);
        token.validarUso(TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, ahora);
        if (!tokens.marcarComoUsado(token.id(), ahora)) {
            throw new EnlaceInvalido();
        }
        Usuario usuario = usuarios.buscarPorId(token.usuarioId()).orElseThrow(EnlaceInvalido::new);

        // Abrir un enlace que llegó al correo también prueba que el correo es suyo: si la cuenta
        // estaba sin verificar, queda verificada. Así quien perdió el correo de verificación puede
        // entrar usando "recuperar contraseña", sin un endpoint extra.
        usuarios.actualizar(usuario.conContrasena(cifrador.cifrar(comando.contrasenaNueva())).conCorreoVerificado());
        tokens.invalidarPendientes(usuario.id(), TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, ahora);
        // Si alguien más tenía la sesión abierta (por eso se cambia la clave), la pierde.
        sesiones.revocarTodasDe(usuario.id(), ahora);
    }
}
