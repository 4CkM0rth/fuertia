package app.fuertia.identidad.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del dominio de los tokens: secreto, hash, enlaces de un uso y renovación. */
class TokensTest {

    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");

    @Test
    void elSecretoTiene256BitsEnBase64UrlYNoSeRepite() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String valor = TokenSecreto.generar().valor();
            // 32 bytes en base64url sin relleno son 43 caracteres, solo letras, números, "-" y "_".
            assertThat(valor).hasSize(43).matches("^[A-Za-z0-9_-]+$");
            assertThat(vistos.add(valor)).isTrue();
        }
    }

    @Test
    void elSecretoNoSeImprime() {
        TokenSecreto secreto = TokenSecreto.generar();

        assertThat(secreto.toString()).doesNotContain(secreto.valor());
    }

    @Test
    void leerRechazaLoQueNoTieneElFormato() {
        assertThat(TokenSecreto.leer(null)).isEmpty();
        assertThat(TokenSecreto.leer("")).isEmpty();
        assertThat(TokenSecreto.leer("a".repeat(42))).isEmpty();
        assertThat(TokenSecreto.leer("a".repeat(44))).isEmpty();
        assertThat(TokenSecreto.leer("a".repeat(42) + "=")).isEmpty();
        assertThat(TokenSecreto.leer("a".repeat(42) + "/")).isEmpty();
        assertThat(TokenSecreto.leer("a".repeat(43))).isPresent();
    }

    @Test
    void elHashEsSha256EnHexadecimalYSiempreElMismo() {
        TokenSecreto secreto = TokenSecreto.leer("A".repeat(43)).orElseThrow();

        assertThat(secreto.hash().valor()).hasSize(64).matches("^[0-9a-f]+$");
        assertThat(secreto.hash()).isEqualTo(TokenSecreto.leer("A".repeat(43)).orElseThrow().hash());
        assertThat(secreto.hash()).isNotEqualTo(TokenSecreto.generar().hash());
    }

    @Test
    void elEnlaceDeVerificacionDura24HorasYElDeRecuperacion1() {
        Usuario usuario = usuario();

        TokenDeUnUso verificacion = TokenDeUnUso.emitir(TipoDeTokenDeUnUso.VERIFICACION_CORREO, usuario, hash(), AHORA);
        TokenDeUnUso recuperacion = TokenDeUnUso.emitir(TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, usuario, hash(), AHORA);

        assertThat(verificacion.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(24)));
        assertThat(recuperacion.venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(1)));
        assertThat(verificacion.tenantId()).isEqualTo(usuario.tenantId());
    }

    @Test
    void unEnlaceSoloSirveSinUsarAntesDeVencerYParaSuTipo() {
        TokenDeUnUso token = TokenDeUnUso.emitir(TipoDeTokenDeUnUso.VERIFICACION_CORREO, usuario(), hash(), AHORA);

        token.validarUso(TipoDeTokenDeUnUso.VERIFICACION_CORREO, AHORA.plus(Duration.ofHours(23)));

        assertThatThrownBy(() -> token.validarUso(TipoDeTokenDeUnUso.VERIFICACION_CORREO, token.venceEn()))
                .isInstanceOf(EnlaceInvalido.class);
        assertThatThrownBy(() -> token.validarUso(TipoDeTokenDeUnUso.RECUPERACION_CONTRASENA, AHORA))
                .isInstanceOf(EnlaceInvalido.class);
        TokenDeUnUso usado = new TokenDeUnUso(token.id(), token.tipo(), token.usuarioId(), token.tenantId(),
                token.hash(), token.creadoEn(), token.venceEn(), AHORA);
        assertThatThrownBy(() -> usado.validarUso(TipoDeTokenDeUnUso.VERIFICACION_CORREO, AHORA))
                .isInstanceOf(EnlaceInvalido.class);
    }

    @Test
    void elTokenDeRenovacionDura30DiasYUsadoGanaAVencido() {
        Sesion sesion = Sesion.abrir(usuario(), Canal.WEB, AHORA);
        TokenDeRenovacion token = TokenDeRenovacion.emitir(sesion, hash(), AHORA);

        assertThat(token.venceEn()).isEqualTo(AHORA.plus(Duration.ofDays(30)));
        assertThat(token.tenantId()).isEqualTo(sesion.tenantId());
        assertThat(token.estado(AHORA)).isEqualTo(TokenDeRenovacion.Estado.VIGENTE);
        assertThat(token.estado(token.venceEn())).isEqualTo(TokenDeRenovacion.Estado.VENCIDO);

        TokenDeRenovacion usado = new TokenDeRenovacion(token.id(), token.sesionId(), token.tenantId(), token.hash(),
                token.creadoEn(), token.venceEn(), AHORA);
        // Un token usado que vuelve a llegar es señal de robo, aunque además esté vencido.
        assertThat(usado.estado(token.venceEn().plusSeconds(1))).isEqualTo(TokenDeRenovacion.Estado.USADO);
    }

    @Test
    void elUsuarioNuevoArrancaSinVerificarYLosCambiosDevuelvenCopias() {
        Usuario usuario = usuario();

        assertThat(usuario.verificado()).isFalse();
        Usuario verificado = usuario.conCorreoVerificado();
        assertThat(verificado.verificado()).isTrue();
        assertThat(usuario.verificado()).as("el original no cambia").isFalse();
        assertThat(verificado.conContrasena("otro-hash").contrasenaHash()).isEqualTo("otro-hash");
        assertThat(verificado.conContrasena("otro-hash").verificado()).isTrue();
    }

    private static Usuario usuario() {
        return Usuario.nuevoEntrenador(TenantId.nuevo(), "Ana Pérez", new Correo("ana@correo.com"), "{argon2}x", AHORA);
    }

    private static HashDeToken hash() {
        return TokenSecreto.generar().hash();
    }
}
