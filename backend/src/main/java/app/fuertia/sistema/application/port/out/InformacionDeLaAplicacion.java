package app.fuertia.sistema.application.port.out;

/** De dónde salen el nombre y la versión. Hoy de la configuración y del build; mañana podría ser otra fuente. */
public interface InformacionDeLaAplicacion {

    String nombre();

    String version();
}
