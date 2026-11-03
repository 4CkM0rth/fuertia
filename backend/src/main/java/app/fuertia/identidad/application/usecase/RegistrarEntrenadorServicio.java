package app.fuertia.identidad.application.usecase;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.CorreoYaRegistrado;
import app.fuertia.identidad.domain.PoliticaDeContrasena;
import app.fuertia.identidad.domain.Tenant;
import app.fuertia.identidad.domain.Usuario;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Caso de uso: registrar un entrenador nuevo. Crea su negocio (tenant) y su usuario con rol ENTRENADOR.
 *
 * Es Java puro: no tiene anotaciones de Spring ni sabe de transacciones. La transacción y el
 * contexto de tenant los pone la infraestructura al conectarlo (ver IdentidadConfig).
 * Por eso se puede probar con dobles de los puertos, sin levantar nada.
 */
public final class RegistrarEntrenadorServicio implements RegistrarEntrenador {

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeTenants tenants;
    private final CifradorDeContrasenas cifrador;
    // Se inyecta el reloj para que las pruebas puedan fijar la hora.
    private final Clock reloj;

    public RegistrarEntrenadorServicio(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants,
                                       CifradorDeContrasenas cifrador, Clock reloj) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.tenants = Objects.requireNonNull(tenants);
        this.cifrador = Objects.requireNonNull(cifrador);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public Resultado registrar(Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio");
        Correo correo = new Correo(comando.correo());
        PoliticaDeContrasena.validar(comando.contrasena());
        if (usuarios.existeCorreo(correo)) {
            throw new CorreoYaRegistrado(correo);
        }

        Instant ahora = reloj.instant();
        Tenant tenant = Tenant.nuevo(comando.nombreNegocio(), ahora);
        Usuario entrenador = Usuario.nuevoEntrenador(
                tenant.id(), comando.nombre(), correo, cifrador.cifrar(comando.contrasena()), ahora);

        tenants.guardar(tenant);
        usuarios.guardar(entrenador);

        return new Resultado(tenant.id(), entrenador.id(), entrenador.correo(), entrenador.rol());
    }
}
