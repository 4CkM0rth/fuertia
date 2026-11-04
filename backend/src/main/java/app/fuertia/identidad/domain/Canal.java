package app.fuertia.identidad.domain;

/**
 * Desde dónde se inició la sesión. Decide cómo viaja el token de renovación:
 * WEB en una cookie HttpOnly (el JavaScript de la página no la puede leer) y APP en el cuerpo,
 * porque la app lo guarda en el almacenamiento seguro del teléfono.
 */
public enum Canal {
    WEB,
    APP
}
