package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.domain.CorreoSinVerificar;
import app.fuertia.identidad.domain.CredencialesInvalidas;
import app.fuertia.identidad.domain.DatoInvalido;
import app.fuertia.identidad.domain.DemasiadosIntentos;
import app.fuertia.identidad.domain.EnlaceInvalido;
import app.fuertia.identidad.domain.SesionInvalida;
import app.fuertia.identidad.infrastructure.config.CifradorOcupado;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Traduce las excepciones del dominio de identidad a Problem Details (RFC 9457).
 *
 * Va antes que el manejador global (HIGHEST_PRECEDENCE) porque Spring usa el primer
 * @RestControllerAdvice que sepa manejar la excepción, y el global atrapa cualquier Exception.
 * Los errores genéricos (validación del formulario, JSON mal formado, 500) los maneja
 * sistema.infrastructure.web.ManejadorGlobalDeErrores.
 *
 * "codigo" es un texto estable (ENLACE_INVALIDO, CORREO_SIN_VERIFICAR) para que la web y la app
 * decidan qué pantalla mostrar sin depender del título, que puede cambiar de redacción.
 */
@RestControllerAdvice(basePackages = "app.fuertia.identidad")
@Order(Ordered.HIGHEST_PRECEDENCE)
class ManejadorDeErroresDeIdentidad {

    // Reglas del dominio que el formulario no alcanzó a revisar (por ejemplo "ana@localhost",
    // que @Email acepta pero Correo no). Se responden igual que un error de validación.
    @ExceptionHandler(DatoInvalido.class)
    ProblemDetail datoInvalido(DatoInvalido error) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos");
        problema.setDetail("Revisa los campos marcados.");
        problema.setProperty("errores", Map.of(error.campo(), error.getMessage()));
        return problema;
    }

    @ExceptionHandler(EnlaceInvalido.class)
    ProblemDetail enlaceInvalido(EnlaceInvalido error) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "El enlace no es válido o ya venció");
        problema.setDetail("Pide un enlace nuevo.");
        problema.setProperty("codigo", "ENLACE_INVALIDO");
        return problema;
    }

    @ExceptionHandler(CredencialesInvalidas.class)
    ProblemDetail credencialesInvalidas(CredencialesInvalidas error) {
        return problema(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }

    @ExceptionHandler(CorreoSinVerificar.class)
    ProblemDetail correoSinVerificar(CorreoSinVerificar error) {
        ProblemDetail problema = problema(HttpStatus.FORBIDDEN, "Confirma tu correo para entrar");
        problema.setDetail("Abre el enlace que te enviamos al correo.");
        problema.setProperty("codigo", "CORREO_SIN_VERIFICAR");
        return problema;
    }

    @ExceptionHandler(SesionInvalida.class)
    ProblemDetail sesionInvalida(SesionInvalida error) {
        return problema(HttpStatus.UNAUTHORIZED, "Tu sesión no es válida. Inicia sesión otra vez.");
    }

    @ExceptionHandler(DemasiadosIntentos.class)
    ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentos error) {
        // Retry-After en segundos enteros, redondeando hacia arriba y nunca 0.
        long segundos = Math.max(1, (error.reintentarEn().toMillis() + 999) / 1000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(segundos))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS,
                        "Hiciste demasiados intentos. Espera un momento y vuelve a probar."));
    }

    @ExceptionHandler(CifradorOcupado.class)
    ResponseEntity<ProblemDetail> cifradorOcupado(CifradorOcupado error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(problema(HttpStatus.SERVICE_UNAVAILABLE,
                        "Estamos atendiendo muchas solicitudes. Vuelve a probar en unos segundos."));
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo) {
        ProblemDetail problema = ProblemDetail.forStatus(estado);
        problema.setTitle(titulo);
        return problema;
    }
}
