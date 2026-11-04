package app.fuertia.sistema.infrastructure.config;

import app.fuertia.sistema.application.port.out.InformacionDeLaAplicacion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

/**
 * Adaptador del puerto InformacionDeLaAplicacion.
 * El nombre sale de spring.application.name y la versión del build-info.properties que genera
 * Gradle (springBoot { buildInfo() } en build.gradle.kts), así nunca hay que escribirla a mano.
 */
final class InformacionDesdeElBuild implements InformacionDeLaAplicacion {

    private static final String VERSION_DESCONOCIDA = "desconocida";

    private final String nombre;
    private final ObjectProvider<BuildProperties> build;

    InformacionDesdeElBuild(String nombre, ObjectProvider<BuildProperties> build) {
        this.nombre = nombre;
        this.build = build;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    @Override
    public String version() {
        // Puede faltar si alguien arranca la app desde el IDE sin pasar por Gradle.
        BuildProperties propiedades = build.getIfAvailable();
        return propiedades != null && propiedades.getVersion() != null ? propiedades.getVersion() : VERSION_DESCONOCIDA;
    }
}
