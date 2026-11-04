package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.domain.Correo;
import app.fuertia.identidad.domain.SesionId;
import app.fuertia.identidad.domain.TenantId;
import app.fuertia.identidad.domain.Usuario;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El JWT de acceso de punta a punta, sin Spring: se emite con EmisorJwt y se verifica con el
 * mismo decodificador que usa la API. Aquí se prueban las reglas que rechazan tokens falsos.
 */
class JwtTest {

    private final LlavesDeJwt llaves = LlavesDeJwt.temporales();
    private final JwtEncoder codificador = FabricaDeJwt.codificador(llaves);
    private final JwtDecoder decodificador = FabricaDeJwt.decodificador(llaves);

    @Test
    void elTokenEmitidoSeVerificaYTraeLosClaims() {
        Usuario usuario = Usuario.nuevoEntrenador(TenantId.nuevo(), "Ana Pérez", new Correo("ana@correo.com"),
                "{argon2}x", Instant.now());
        SesionId sesion = SesionId.nueva();
        Instant ahora = Instant.now();

        var token = new EmisorJwt(codificador).emitir(usuario, sesion, ahora);
        Jwt jwt = decodificador.decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo(usuario.id().toString());
        assertThat(jwt.getClaimAsString("tid")).isEqualTo(usuario.tenantId().toString());
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("ENTRENADOR");
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(sesion.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("fuertia");
        assertThat(jwt.getAudience()).containsExactly("fuertia-api");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("alg", "ES256").containsEntry("typ", "at+jwt");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(token.venceEn()).isEqualTo(ahora.plus(Duration.ofMinutes(15)));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraLlave() {
        JwtEncoder otro = FabricaDeJwt.codificador(LlavesDeJwt.temporales());

        assertThatThrownBy(() -> decodificador.decode(firmar(otro, claims -> {
        }))).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenVencido() {
        String vencido = firmar(codificador, claims -> claims
                .issuedAt(Instant.now().minus(Duration.ofMinutes(30)))
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5))));

        assertThatThrownBy(() -> decodificador.decode(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaOtraAudienciaYOtroEmisor() {
        assertThatThrownBy(() -> decodificador.decode(firmar(codificador, c -> c.audience(List.of("otra-api")))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificador.decode(firmar(codificador, c -> c.issuer("otro"))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokensSinLosClaimsDeFuertiaOConValoresRaros() {
        assertThatThrownBy(() -> decodificador.decode(firmar(codificador, c -> c.claim("tid", "no-es-uuid"))))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decodificador.decode(firmar(codificador, c -> c.claim("rol", "SUPERUSUARIO"))))
                .isInstanceOf(JwtException.class);
        // El tenant centinela "sistema" apagaría el filtro: nunca puede venir en un token.
        assertThatThrownBy(() -> decodificador.decode(firmar(codificador,
                c -> c.claim("tid", TenantId.RESERVADO_SISTEMA.toString())))).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaOtroTipoDeJwtAunqueEsteBienFirmado() {
        String sinTipo = codificador.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).build(), claimsValidos().build())).getTokenValue();

        assertThatThrownBy(() -> decodificador.decode(sinTipo)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaElAlgoritmoNoneYHs256() throws Exception {
        JWTClaimsSet claims = JWTClaimsSet.parse(claimsValidos().build().getClaims().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, entrada ->
                        entrada.getValue() instanceof Instant instante ? instante.getEpochSecond() : entrada.getValue())));

        String sinFirma = new PlainJWT(claims).serialize();
        assertThatThrownBy(() -> decodificador.decode(sinFirma)).isInstanceOf(JwtException.class);

        // HS256 firmado con la llave pública como si fuera un secreto: el ataque clásico de "confusión de algoritmo".
        SignedJWT hs256 = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(new JOSEObjectType("at+jwt")).build(), claims);
        hs256.sign(new MACSigner(java.util.Arrays.copyOf(llaves.publica().getEncoded(), 64)));
        assertThatThrownBy(() -> decodificador.decode(hs256.serialize())).isInstanceOf(JwtException.class);
    }

    @Test
    void cargarUnaLlavePemDevuelveLaMismaLlavePublica() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp256r1"));
        var par = generador.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(par.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        LlavesDeJwt cargadas = LlavesDeJwt.cargar(pem, false);

        // La pública se calcula desde la privada: debe ser exactamente la del par original.
        assertThat(cargadas.publica().getW()).isEqualTo(((java.security.interfaces.ECPublicKey) par.getPublic()).getW());
        // Y también acepta los saltos de línea escritos como "\n", como llegan a veces en variables de entorno.
        assertThat(LlavesDeJwt.cargar(pem.replace("\n", "\\n"), false).publica().getW())
                .isEqualTo(cargadas.publica().getW());
    }

    @Test
    void rechazaUnaLlaveQueNoEsP256() throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
        generador.initialize(new ECGenParameterSpec("secp384r1"));
        String pem = Base64.getEncoder().encodeToString(generador.generateKeyPair().getPrivate().getEncoded());

        assertThatThrownBy(() -> LlavesDeJwt.cargar(pem, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("P-256");
    }

    @Test
    void sinLlaveFueraDelPerfilLocalNoArranca() {
        assertThatThrownBy(() -> LlavesDeJwt.cargar("", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FUERTIA_JWT_LLAVE_PRIVADA");
        assertThatThrownBy(() -> LlavesDeJwt.cargar(null, false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sinLlaveEnElPerfilLocalUsaUnaTemporal() {
        LlavesDeJwt temporales = LlavesDeJwt.cargar(null, true);

        assertThat(temporales.publica()).isNotNull();
        assertThat(temporales.toString()).doesNotContain(temporales.privada().getS().toString());
    }

    private static JwtClaimsSet.Builder claimsValidos() {
        Instant ahora = Instant.now();
        return JwtClaimsSet.builder()
                .issuer("fuertia")
                .audience(List.of("fuertia-api"))
                .subject(UUID.randomUUID().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofMinutes(15)))
                .id(UUID.randomUUID().toString())
                .claim("tid", UUID.randomUUID().toString())
                .claim("rol", "ENTRENADOR")
                .claim("sid", UUID.randomUUID().toString());
    }

    private static String firmar(JwtEncoder codificador, Consumer<JwtClaimsSet.Builder> cambio) {
        JwtClaimsSet.Builder claims = claimsValidos();
        cambio.accept(claims);
        JwsHeader encabezado = JwsHeader.with(SignatureAlgorithm.ES256).type("at+jwt").build();
        return codificador.encode(JwtEncoderParameters.from(encabezado, claims.build())).getTokenValue();
    }
}
