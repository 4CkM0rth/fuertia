package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.EnviadorDeCorreos;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.PoliticaDeContrasena;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.TipoDeTokenDeUnUso;
import app.fuertia.identidad.domain.TokenDeUnUso;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Caso de uso: registrar un entrenador nuevo.
 *
 * Desde afuera los dos caminos se ven iguales (202 con el mismo mensaje), para que nadie pueda
 * averiguar qué correos tienen cuenta:
 * - Correo nuevo: crea el tenant y el usuario sin verificar, y envía el enlace de verificación.
 * - Correo existente: no crea nada y envía el aviso "ya tienes una cuenta".
 *
 * Para que tampoco el TIEMPO de respuesta los delate, la contraseña se cifra con Argon2 en los
 * dos caminos (es lo único lento: decenas de milisegundos) y el correo se envía fuera de la
 * petición en los dos. Lo que queda distinto son tres INSERT de pocos milisegundos.
 *
 * Es Java puro: la transacción y el contexto de tenant los pone la infraestructura (IdentidadConfig).
 */
public final class RegistrarEntrenadorServicio implements RegistrarEntrenador {

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeTenants tenants;
    private final RepositorioDeTokensDeUnUso tokens;
    private final CifradorDeContrasenas cifrador;
    private final EnviadorDeCorreos correos;
    // Se inyecta el reloj para que las pruebas puedan fijar la hora.
    private final Clock reloj;

    public RegistrarEntrenadorServicio(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants,
                                       RepositorioDeTokensDeUnUso tokens, CifradorDeContrasenas cifrador,
                                       EnviadorDeCorreos correos, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.tenants = Objects.requireNonNull(tenants);
        this.tokens = Objects.requireNonNull(tokens);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.correos = Objects.requireNonNull(correos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Resultado registrar(Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio");
        // Primero todas las validaciones, para que un dato malo responda 400 igual en los dos caminos.
        Correo correo = new Correo(comando.correo());
        PoliticaDeContrasena.validar(comando.contrasena());
        String nombre = Usuario.validarNombre(comando.nombre());
        Instant ahora = reloj.instant();
        Tenant tenant = Tenant.nuevo(comando.nombreNegocio(), ahora);

        // Siempre: así el camino del correo existente cuesta lo mismo que el del correo nuevo.
        String hash = cifrador.cifrar(comando.contrasena());

        if (usuarios.existeCorreo(correo)) {
            correos.enviarAvisoDeCuentaExistente(correo);
            return new CuentaExistente(correo);
        }

        Usuario entrenador = Usuario.nuevoEntrenador(tenant.id(), nombre, correo, hash, ahora);
        tenants.guardar(tenant);
        usuarios.guardar(entrenador);

        TokenSecreto secreto = TokenSecreto.generar();
        tokens.guardar(TokenDeUnUso.emitir(TipoDeTokenDeUnUso.VERIFICACION_CORREO, entrenador, secreto.hash(), ahora));
        correos.enviarEnlaceDeVerificacion(correo, entrenador.nombre(), secreto);

        return new CuentaCreada(tenant.id(), entrenador.id(), correo);
    }
}
