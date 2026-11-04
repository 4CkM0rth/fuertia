package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.VerificarCorreo;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.EnlaceInvalido;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class VerificarCorreoServicio implements VerificarCorreo {

    private final RepositorioDeTokensDeUnUso tokens;
    private final RepositorioDeUsuarios usuarios;
    private final Clock reloj;

    public VerificarCorreoServicio(RepositorioDeTokensDeUnUso tokens, RepositorioDeUsuarios usuarios, Clock reloj) {
        this.tokens = Objects.requireNonNull(tokens);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void verificar(String textoDelToken) {
        Instant ahora = reloj.instant();
        TokenSecreto secreto = TokenSecreto.leer(textoDelToken).orElseThrow(EnlaceInvalido::new);
        TokenDeUnUso token = tokens.buscarPorHash(secreto.hash()).orElseThrow(EnlaceInvalido::new);
        token.validarUso(TipoDeTokenDeUnUso.VERIFICACION_CORREO, ahora);
        if (!tokens.marcarComoUsado(token.id(), ahora)) {
            throw new EnlaceInvalido();
        }
        Usuario usuario = usuarios.buscarPorId(token.usuarioId()).orElseThrow(EnlaceInvalido::new);
        usuarios.actualizar(usuario.conCorreoVerificado());
    }
}
