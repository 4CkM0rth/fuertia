package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.IniciarSesion;
import app.fuertia.identidad.application.port.in.SesionIniciada;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.CorreoSinVerificar;
import app.fuertia.identidad.domain.CredencialesInvalidas;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class IniciarSesionServicio implements IniciarSesion {

    // Por correo, además del límite por IP que pone la capa web: frena a quien prueba claves contra
    // UNA cuenta desde muchas IP. Se cuenta antes de Argon2, así tampoco sirve para gastar CPU.
    static final int MAXIMO_POR_CORREO = 5;
    static final Duration VENTANA_POR_CORREO = Duration.ofMinutes(15);

    private final RepositorioDeUsuarios usuarios;
    private final CifradorDeContrasenas cifrador;
    private final LimitadorDePeticiones limitador;
    private final EmisionDeSesion emision;
    private final Clock reloj;

    public IniciarSesionServicio(RepositorioDeUsuarios usuarios, CifradorDeContrasenas cifrador,
                                 LimitadorDePeticiones limitador, EmisionDeSesion emision, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.limitador = Objects.requireNonNull(limitador);
        this.emision = Objects.requireNonNull(emision);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public SesionIniciada iniciar(Comando comando) {
        Objects.requireNonNull(comando);
        Correo correo = new Correo(comando.correo());
        String contrasena = Objects.requireNonNullElse(comando.contrasena(), "");
        limitador.consumir("inicio-sesion:correo:" + correo.valor(), MAXIMO_POR_CORREO, VENTANA_POR_CORREO);

        Optional<Usuario> encontrado = usuarios.buscarPorCorreo(correo);
        if (encontrado.isEmpty()) {
            // Hash señuelo: el mismo trabajo de Argon2 que con una cuenta real. Sin esto, un correo
            // inexistente respondería en 2 ms y uno existente en 60 ms, y el tiempo lo delataría.
            cifrador.coincide(contrasena, cifrador.hashSenuelo());
            throw new CredencialesInvalidas();
        }
        Usuario usuario = encontrado.get();
        if (!cifrador.coincide(contrasena, usuario.contrasenaHash())) {
            throw new CredencialesInvalidas();
        }
        // Solo después de comprobar la clave: decir "sin verificar" a quien no sabe la clave
        // revelaría que el correo existe.
        if (!usuario.verificado()) {
            throw new CorreoSinVerificar();
        }
        return emision.abrir(usuario, comando.canal(), reloj.instant());
    }
}
