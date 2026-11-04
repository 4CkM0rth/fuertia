package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.SolicitarRecuperacion;
import app.fuertia.identidad.application.port.out.EnviadorDeCorreos;
import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class SolicitarRecuperacionServicio implements SolicitarRecuperacion {

    // Evita usar Fuertia para llenarle el buzón a alguien.
    static final int MAXIMO_POR_CORREO = 3;
    static final Duration VENTANA_POR_CORREO = Duration.ofHours(1);

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeTokensDeUnUso tokens;
    private final EnviadorDeCorreos correos;
    private final LimitadorDePeticiones limitador;
    private final Clock reloj;

    public SolicitarRecuperacionServicio(RepositorioDeUsuarios usuarios, RepositorioDeTokensDeUnUso tokens,
                                         EnviadorDeCorreos correos, LimitadorDePeticiones limitador, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.tokens = Objects.requireNonNull(tokens);
        this.correos = Objects.requireNonNull(correos);
        this.limitador = Objects.requireNonNull(limitador);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public void solicitar(String textoDelCorreo) {
        Correo correo = new Correo(textoDelCorreo);
        // El límite se aplica exista o no la cuenta: si no, el 429 revelaría cuáles existen.
        limitador.consumir("recuperacion:correo:" + correo.valor(), MAXIMO_POR_CORREO, VENTANA_POR_CORREO);
        Instant ahora = reloj.instant();
        usuarios.buscarPorCorreo(correo).ifPresent(usuario -> {
            // Solo el enlace más reciente sirve: los anteriores quedan sin efecto.
            tokens.invalidarPendientes(usuario.id(), TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, ahora);
            TokenSecreto secreto = TokenSecreto.generar();
            tokens.guardar(TokenDeUnUso.emitir(TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, usuario, secreto.hash(), ahora));
            correos.enviarEnlaceDeRecuperacion(usuario.correo(), usuario.nombre(), secreto);
        });
    }
}
