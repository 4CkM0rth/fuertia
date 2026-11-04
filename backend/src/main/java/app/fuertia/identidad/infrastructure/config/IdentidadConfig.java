package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.in.CerrarSesion;
import app.fuertia.identidad.application.port.in.ConfirmarRecuperacion;
import app.fuertia.identidad.application.port.in.ConsultarUsuarioActual;
import app.fuertia.identidad.application.port.in.IniciarSesion;
import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.in.RenovarSesion;
import app.fuertia.identidad.application.port.in.SolicitarRecuperacion;
import app.fuertia.identidad.application.port.in.VerificarCorreo;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.EmisorDeTokensDeAcceso;
import app.fuertia.identidad.application.port.out.EnviadorDeCorreos;
import app.fuertia.identidad.application.port.out.LimitadorDePeticiones;
import app.fuertia.identidad.application.port.out.RepositorioDeSesiones;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeRenovacion;
import app.fuertia.identidad.application.port.out.RepositorioDeTokensDeUnUso;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.application.usecase.CerrarSesionServicio;
import app.fuertia.identidad.application.usecase.ConfirmarRecuperacionServicio;
import app.fuertia.identidad.application.usecase.ConsultarUsuarioActualServicio;
import app.fuertia.identidad.application.usecase.EmisionDeSesion;
import app.fuertia.identidad.application.usecase.IniciarSesionServicio;
import app.fuertia.identidad.application.usecase.RegistrarEntrenadorServicio;
import app.fuertia.identidad.application.usecase.RenovarSesionServicio;
import app.fuertia.identidad.application.usecase.SolicitarRecuperacionServicio;
import app.fuertia.identidad.application.usecase.VerificarCorreoServicio;
import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.TokenSecreto;
import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import app.fuertia.identidad.infrastructure.persistence.DescubridorDeTenant;
import app.fuertia.identidad.infrastructure.persistence.ResolvedorDeTenantActual;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Conecta las piezas del módulo. Los casos de uso no llevan @Service (son Java puro), así que
 * se crean aquí a mano, pasándoles los adaptadores que implementan sus puertos, y se envuelven
 * con la transacción y el tenant que les corresponde (EjecutorDeCasosDeUso).
 */
@Configuration(proxyBeanMethods = false)
class IdentidadConfig {

    // --- Multi-tenant ---

    @Bean
    ContextoDeTenant contextoDeTenant() {
        return new ContextoDeTenant();
    }

    @Bean
    ResolvedorDeTenantActual resolvedorDeTenantActual(ContextoDeTenant contexto) {
        return new ResolvedorDeTenantActual(contexto);
    }

    /** Registra el resolvedor en Hibernate: esto es lo que enciende el filtro global por tenant. */
    @Bean
    HibernatePropertiesCustomizer multiTenantPorDiscriminador(ResolvedorDeTenantActual resolvedor) {
        return propiedades -> propiedades.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolvedor);
    }

    @Bean
    EjecutorDeCasosDeUso ejecutorDeCasosDeUso(ContextoDeTenant contexto, PlatformTransactionManager transacciones) {
        return new EjecutorDeCasosDeUso(contexto, new TransactionTemplate(transacciones));
    }

    // --- Adaptadores ---

    @Bean
    Clock relojDeIdentidad() {
        return Clock.systemUTC();
    }

    @Bean
    LlavesDeJwt llavesDeJwt(PropiedadesDeIdentidad propiedades, Environment entorno) {
        return LlavesDeJwt.cargar(propiedades.jwt().llavePrivada(), entorno.matchesProfiles("local"));
    }

    @Bean
    JwtEncoder codificadorDeJwt(LlavesDeJwt llaves) {
        return FabricaDeJwt.codificador(llaves);
    }

    @Bean
    JwtDecoder decodificadorDeJwt(LlavesDeJwt llaves) {
        return FabricaDeJwt.decodificador(llaves);
    }

    @Bean
    EmisorDeTokensDeAcceso emisorDeTokensDeAcceso(JwtEncoder codificador) {
        return new EmisorJwt(codificador);
    }

    @Bean
    CifradorDeContrasenas cifradorDeContrasenas(PropiedadesDeIdentidad propiedades) {
        return new CifradorArgon2(propiedades.argon2().simultaneos(), propiedades.argon2().espera());
    }

    /**
     * Los correos salen por un grupo de hilos propio. No se registra como bean aparte a propósito:
     * si hubiera un bean Executor, Spring Boot dejaría de crear el suyo para el resto de la app.
     * Spring llama a close() del enviador al apagar, y ese close espera los correos pendientes.
     */
    @Bean
    EnviadorDeCorreosSmtp enviadorDeCorreos(JavaMailSender servidor, PropiedadesDeIdentidad propiedades) {
        ThreadPoolTaskExecutor hilos = new ThreadPoolTaskExecutor();
        hilos.setThreadNamePrefix("correo-");
        hilos.setCorePoolSize(2);
        hilos.setMaxPoolSize(4);
        hilos.setQueueCapacity(1000);
        hilos.setWaitForTasksToCompleteOnShutdown(true);
        hilos.setAwaitTerminationSeconds(10);
        hilos.initialize();
        var correo = propiedades.correo();
        return new EnviadorDeCorreosSmtp(servidor, hilos, new PlantillasDeCorreo(correo.urlWeb()), correo.remitente());
    }

    @Bean
    EmisionDeSesion emisionDeSesion(RepositorioDeSesiones sesiones, RepositorioDeTokensDeRenovacion tokensDeRenovacion,
                                    RepositorioDeTenants tenants, EmisorDeTokensDeAcceso emisor) {
        return new EmisionDeSesion(sesiones, tokensDeRenovacion, tenants, emisor);
    }

    // --- Casos de uso, cada uno con su transacción y su tenant ---

    @Bean
    RegistrarEntrenador registrarEntrenador(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants,
                                            RepositorioDeTokensDeUnUso tokens, CifradorDeContrasenas cifrador,
                                            EnviadorDeCorreos correos, ContextoDeTenant contexto,
                                            PlatformTransactionManager transacciones, Clock reloj) {
        var casoDeUso = new RegistrarEntrenadorServicio(usuarios, tenants, tokens, cifrador, correos, reloj);
        return new RegistrarEntrenadorTransaccional(casoDeUso, contexto, new TransactionTemplate(transacciones));
    }

    @Bean
    VerificarCorreo verificarCorreo(RepositorioDeTokensDeUnUso tokens, RepositorioDeUsuarios usuarios,
                                    DescubridorDeTenant descubridor, EjecutorDeCasosDeUso ejecutor, Clock reloj) {
        var casoDeUso = new VerificarCorreoServicio(tokens, usuarios, reloj);
        return token -> ejecutor.enTenantSinResultado(
                TokenSecreto.leer(token).flatMap(secreto -> descubridor.porTokenDeUnUso(secreto.hash())),
                () -> casoDeUso.verificar(token));
    }

    @Bean
    IniciarSesion iniciarSesion(RepositorioDeUsuarios usuarios, CifradorDeContrasenas cifrador,
                                LimitadorDePeticiones limitador, EmisionDeSesion emision,
                                DescubridorDeTenant descubridor, EjecutorDeCasosDeUso ejecutor, Clock reloj) {
        var casoDeUso = new IniciarSesionServicio(usuarios, cifrador, limitador, emision, reloj);
        return comando -> ejecutor.enTenant(
                descubridor.porCorreo(new Correo(comando.correo())),
                () -> casoDeUso.iniciar(comando));
    }

    @Bean
    RenovarSesion renovarSesion(RepositorioDeTokensDeRenovacion tokens, RepositorioDeSesiones sesiones,
                                RepositorioDeUsuarios usuarios, EmisionDeSesion emision,
                                DescubridorDeTenant descubridor, EjecutorDeCasosDeUso ejecutor, Clock reloj) {
        var casoDeUso = new RenovarSesionServicio(tokens, sesiones, usuarios, emision, reloj);
        return comando -> ejecutor.enTenant(
                TokenSecreto.leer(comando.tokenDeRenovacion())
                        .flatMap(secreto -> descubridor.porTokenDeRenovacion(secreto.hash())),
                () -> casoDeUso.renovar(comando));
    }

    @Bean
    CerrarSesion cerrarSesion(RepositorioDeSesiones sesiones, EjecutorDeCasosDeUso ejecutor, Clock reloj) {
        var casoDeUso = new CerrarSesionServicio(sesiones, reloj);
        return sesion -> ejecutor.enTransaccionSinResultado(() -> casoDeUso.cerrar(sesion));
    }

    @Bean
    SolicitarRecuperacion solicitarRecuperacion(RepositorioDeUsuarios usuarios, RepositorioDeTokensDeUnUso tokens,
                                                EnviadorDeCorreos correos, LimitadorDePeticiones limitador,
                                                DescubridorDeTenant descubridor, EjecutorDeCasosDeUso ejecutor,
                                                Clock reloj) {
        var casoDeUso = new SolicitarRecuperacionServicio(usuarios, tokens, correos, limitador, reloj);
        return correo -> ejecutor.enTenantSinResultado(
                descubridor.porCorreo(new Correo(correo)),
                () -> casoDeUso.solicitar(correo));
    }

    @Bean
    ConfirmarRecuperacion confirmarRecuperacion(RepositorioDeTokensDeUnUso tokens, RepositorioDeUsuarios usuarios,
                                                RepositorioDeSesiones sesiones, CifradorDeContrasenas cifrador,
                                                DescubridorDeTenant descubridor, EjecutorDeCasosDeUso ejecutor,
                                                Clock reloj) {
        var casoDeUso = new ConfirmarRecuperacionServicio(tokens, usuarios, sesiones, cifrador, reloj);
        return comando -> ejecutor.enTenantSinResultado(
                TokenSecreto.leer(comando.token()).flatMap(secreto -> descubridor.porTokenDeUnUso(secreto.hash())),
                () -> casoDeUso.confirmar(comando));
    }

    @Bean
    ConsultarUsuarioActual consultarUsuarioActual(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants,
                                                  EjecutorDeCasosDeUso ejecutor) {
        var casoDeUso = new ConsultarUsuarioActualServicio(usuarios, tenants);
        return usuario -> ejecutor.enTransaccion(() -> casoDeUso.consultar(usuario));
    }
}
