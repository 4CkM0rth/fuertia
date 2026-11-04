package app.fuertia.identidad.application.port.in;

import app.fuertia.identidad.application.port.out.EmisorDeTokensDeAcceso.TokenDeAcceso;
import app.fuertia.identidad.domain.Canal;
import app.fuertia.identidad.domain.TokenSecreto;

/**
 * Resultado de iniciar o renovar una sesión. La capa web decide cómo entregar el token de
 * renovación según el canal: en una cookie (WEB) o en el cuerpo (APP).
 */
public record SesionIniciada(TokenDeAcceso acceso, TokenSecreto tokenDeRenovacion, Canal canal,
                             UsuarioActual usuario) {
}
