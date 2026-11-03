package app.fuertia.identidad.infrastructure.web;

import app.fuertia.identidad.domain.CorreoYaRegistrado;
import app.fuertia.identidad.domain.DatoInvalido;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
 */
@RestControllerAdvice(basePackages = "app.fuertia.identidad")
@Order(Ordered.HIGHEST_PRECEDENCE)
class ManejadorDeErroresDeIdentidad {

    @ExceptionHandler(CorreoYaRegistrado.class)
    ProblemDetail correoYaRegistrado(CorreoYaRegistrado error) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Ya existe una cuenta con ese correo");
        problema.setDetail("Inicia sesión o usa otro correo.");
        return problema;
    }

    // Reglas del dominio que el formulario no alcanzó a revisar (por ejemplo "ana@localhost",
    // que @Email acepta pero Correo no). Se responden igual que un error de validación.
    @ExceptionHandler(DatoInvalido.class)
    ProblemDetail datoInvalido(DatoInvalido error) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Datos inválidos");
        problema.setDetail("Revisa los campos marcados.");
        problema.setProperty("errores", Map.of(error.campo(), error.getMessage()));
        return problema;
    }
}
