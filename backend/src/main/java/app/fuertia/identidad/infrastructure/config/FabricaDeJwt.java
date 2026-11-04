package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.Rol;
import app.fuertia.identidad.domain.TenantId;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Arma el codificador (firma) y el decodificador (verifica) de los JWT de acceso.
 * Son métodos estáticos para poder probarlos sin levantar Spring (FabricaDeJwtTest).
 */
public final class FabricaDeJwt {

    public static final String EMISOR = "fuertia";
    public static final String AUDIENCIA = "fuertia-api";
    /** "Este JWT es un token de acceso" (RFC 9068). Evita aceptar otro tipo de JWT que alguien firme con la misma llave. */
    public static final String TIPO = "at+jwt";
    public static final Duration VIGENCIA = Duration.ofMinutes(15);

    public static final String CLAIM_TENANT = "tid";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_SESION = "sid";

    private FabricaDeJwt() {
    }

    public static JwtEncoder codificador(LlavesDeJwt llaves) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk(llaves))));
    }

    /**
     * Verifica, en este orden: el tipo "at+jwt", que el algoritmo sea ES256 (rechaza "none", HS256 y
     * cualquier otro), la firma con nuestra llave pública, y luego las reglas: emisor, audiencia,
     * vencimiento y que estén los claims que usamos.
     */
    public static JwtDecoder decodificador(LlavesDeJwt llaves) {
        DefaultJWTProcessor<SecurityContext> procesador = new DefaultJWTProcessor<>();
        procesador.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType(TIPO)));
        procesador.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.ES256,
                new ImmutableJWKSet<>(new JWKSet(jwk(llaves).toPublicJWK()))));
        // Las reglas de los claims las aplica Spring (abajo), con mensajes de error uniformes.
        procesador.setJWTClaimsSetVerifier((claims, contexto) -> {
        });

        NimbusJwtDecoder decodificador = new NimbusJwtDecoder(procesador);
        // createDefaultWithValidators agrega además el vencimiento (con 60 segundos de tolerancia
        // por relojes desfasados). El tipo se pasa explícito: por defecto Spring solo acepta "JWT".
        decodificador.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtTypeValidator(TIPO),
                new JwtIssuerValidator(EMISOR),
                audiencia(),
                claimsObligatorios()));
        return decodificador;
    }

    private static ECKey jwk(LlavesDeJwt llaves) {
        try {
            return new ECKey.Builder(Curve.P_256, llaves.publica())
                    .privateKey(llaves.privada())
                    .algorithm(JWSAlgorithm.ES256)
                    // El kid identifica la llave; servirá cuando haya que rotarla y convivan dos.
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("No se pudo preparar la llave de JWT", e);
        }
    }

    private static OAuth2TokenValidator<Jwt> audiencia() {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(AUDIENCIA)
                ? OAuth2TokenValidatorResult.success()
                : falla("El token no es para esta API");
    }

    // El filtro por tenant y los permisos dependen de estos claims: sin ellos (o con valores raros)
    // el token no sirve, aunque la firma esté bien.
    private static OAuth2TokenValidator<Jwt> claimsObligatorios() {
        return jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                UUID.fromString(jwt.getClaimAsString(CLAIM_SESION));
                new TenantId(UUID.fromString(jwt.getClaimAsString(CLAIM_TENANT))); // rechaza los ids reservados
                Rol.valueOf(jwt.getClaimAsString(CLAIM_ROL));
                if (jwt.getId() == null || jwt.getIssuedAt() == null || jwt.getExpiresAt() == null) {
                    return falla("Faltan claims obligatorios");
                }
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException e) {
                return falla("Claims inválidos");
            }
        };
    }

    private static OAuth2TokenValidatorResult falla(String descripcion) {
        return OAuth2TokenValidatorResult.failure(List.of(new OAuth2Error("invalid_token", descripcion, null)));
    }
}
