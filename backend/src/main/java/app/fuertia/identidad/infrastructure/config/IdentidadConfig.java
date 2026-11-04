package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.in.RegistrarEntrenador;
import app.fuertia.identidad.application.port.out.CifradorDeContrasenas;
import app.fuertia.identidad.application.port.out.RepositorioDeTenants;
import app.fuertia.identidad.application.port.out.RepositorioDeUsuarios;
import app.fuertia.identidad.application.usecase.RegistrarEntrenadorServicio;
import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import app.fuertia.identidad.infrastructure.persistence.ResolvedorDeTenantActual;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Conecta las piezas del módulo. Los casos de uso no llevan @Service (son Java puro), así que
 * se crean aquí a mano, pasándoles los adaptadores que implementan sus puertos.
 */
@Configuration(proxyBeanMethods = false)
class IdentidadConfig {

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
    CifradorDeContrasenas cifradorDeContrasenas() {
        return new CifradorArgon2();
    }

    @Bean
    RegistrarEntrenador registrarEntrenador(RepositorioDeUsuarios usuarios, RepositorioDeTenants tenants,
                                            CifradorDeContrasenas cifrador, ContextoDeTenant contexto,
                                            PlatformTransactionManager transacciones) {
        var casoDeUso = new RegistrarEntrenadorServicio(usuarios, tenants, cifrador, Clock.systemUTC());
        return new RegistrarEntrenadorTransaccional(casoDeUso, contexto, new TransactionTemplate(transacciones));
    }
}
