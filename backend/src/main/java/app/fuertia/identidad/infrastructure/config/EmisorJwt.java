package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.application.port.out.EmisorDeTokensDeAcceso;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.Usuario;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador del puerto EmisorDeTokensDeAcceso: un JWT firmado con ES256 que dura 15 minutos.
 *
 * El JWT no se guarda en ningún lado: cualquiera de las copias del backend lo verifica con la
 * llave pública, sin consultar la base. Por eso dura poco: si se filtra, sirve máximo 15 minutos.
 */
final class EmisorJwt implements EmisorDeTokensDeAcceso {

    private final JwtEncoder codificador;

    EmisorJwt(JwtEncoder codificador) {
        this.codificador = codificador;
    }

    @Override
    public TokenDeAcceso emitir(Usuario usuario, SesionId sesion, Instant ahora) {
        Instant vence = ahora.plus(FabricaDeJwt.VIGENCIA);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(FabricaDeJwt.EMISOR)
                .audience(List.of(FabricaDeJwt.AUDIENCIA))
                .subject(usuario.id().toString())
                .issuedAt(ahora)
                .expiresAt(vence)
                .id(UUID.randomUUID().toString())
                // El tenant y el rol viajan firmados: el backend los toma SOLO de aquí, nunca de un
                // encabezado o de la URL, que el cliente podría cambiar.
                .claim(FabricaDeJwt.CLAIM_TENANT, usuario.tenantId().toString())
                .claim(FabricaDeJwt.CLAIM_ROL, usuario.rol().name())
                .claim(FabricaDeJwt.CLAIM_SESION, sesion.toString())
                .build();
        JwsHeader encabezado = JwsHeader.with(SignatureAlgorithm.ES256).type(FabricaDeJwt.TIPO).build();
        String valor = codificador.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
        return new TokenDeAcceso(valor, vence);
    }
}
