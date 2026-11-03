package app.fuertia.identidad.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Prueba unitaria pura: sin Spring, sin base de datos. Corre en milisegundos.
class CorreoTest {

    @Test
    void normalizaAMinusculasYQuitaEspacios() {
        Correo correo = new Correo("  Sebastian.Rojas@Ejemplo.COM ");

        assertThat(correo.valor()).isEqualTo("sebastian.rojas@ejemplo.com");
    }

    @Test
    void dosCorreosQueSoloDifierenEnMayusculasSonIguales() {
        assertThat(new Correo("Ana@Correo.com")).isEqualTo(new Correo("ana@correo.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana+fuertia@correo.com.co", "a.b_c-d@sub.dominio.co"})
    void aceptaCorreosValidos(String valor) {
        assertThat(new Correo(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaCorreoVacio(String valor) {
        assertThatThrownBy(() -> new Correo(valor))
                .isInstanceOf(CorreoInvalido.class)
                .hasMessage("Escribe tu correo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba.com", "ana@localhost", "ana@@correo.com", "@correo.com",
            ".ana@correo.com", "ana.@correo.com", "an..a@correo.com", "ana@correo..com", "ana perez@correo.com"})
    void rechazaCorreoConFormatoInvalido(String valor) {
        assertThatThrownBy(() -> new Correo(valor))
                .isInstanceOf(CorreoInvalido.class)
                .hasMessage("Escribe un correo válido");
    }

    @Test
    void rechazaCorreoDemasiadoLargo() {
        String largo = "a".repeat(64) + "@" + "b".repeat(186) + ".com"; // 255 caracteres

        assertThatThrownBy(() -> new Correo(largo))
                .isInstanceOf(CorreoInvalido.class)
                .hasMessageContaining("254");
    }

    @Test
    void elErrorIndicaElCampoCorreo() {
        assertThatThrownBy(() -> new Correo("malo"))
                .isInstanceOfSatisfying(CorreoInvalido.class, error -> assertThat(error.campo()).isEqualTo("correo"));
    }
}
