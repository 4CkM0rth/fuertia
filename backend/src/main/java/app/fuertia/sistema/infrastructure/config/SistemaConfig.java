package app.fuertia.sistema.infrastructure.config;

import app.fuertia.sistema.application.port.in.ConsultarEstado;
import app.fuertia.sistema.application.port.out.InformacionDeLaAplicacion;
import app.fuertia.sistema.application.usecase.ConsultarEstadoServicio;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SistemaConfig {

    @Bean
    InformacionDeLaAplicacion informacionDeLaAplicacion(@Value("${spring.application.name}") String nombre,
                                                        ObjectProvider<BuildProperties> build) {
        return new InformacionDesdeElBuild(nombre, build);
    }

    @Bean
    ConsultarEstado consultarEstado(InformacionDeLaAplicacion informacion) {
        return new ConsultarEstadoServicio(informacion);
    }
}
