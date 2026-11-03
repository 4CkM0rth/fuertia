package app.fuertia.sistema.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Errores comunes a toda la API, en formato Problem Details (RFC 9457) y en español.
 *
 * Heredar de ResponseEntityExceptionHandler hace que Spring ya responda con Problem Details a los
 * errores propios del framework (404, 405, 415...). Aquí solo personalizamos los textos.
 * Los errores de negocio de cada módulo van en el manejador de ese módulo, que corre antes.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class ManejadorGlobalDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

    // Si un campo falla en varias reglas a la vez (vacío y además corto), mostramos la de "obligatorio".
    private static final Set<String> REGLAS_DE_OBLIGATORIO = Set.of("NotBlank", "NotNull", "NotEmpty");

    // Títulos en español para los errores del framework que Spring trae en inglés.
    private static final Map<Integer, String> TITULOS = Map.of(
            400, "Solicitud inválida",
            401, "Necesitas iniciar sesión",
            403, "No tienes permiso para hacer esto",
            404, "No encontramos lo que buscas",
            405, "Método no permitido",
            406, "Formato de respuesta no disponible",
            415, "Tipo de contenido no soportado",
            500, "Ocurrió un error inesperado");

    /** 400 por validación del formulario (@Valid), con el mapa "errores" por campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error,
                                                                  HttpHeaders encabezados, HttpStatusCode estado,
                                                                  WebRequest solicitud) {
        // TreeMap: orden alfabético estable, más fácil de leer y de probar.
        Map<String, String> errores = new TreeMap<>();
        for (FieldError campo : error.getBindingResult().getFieldErrors()) {
            boolean esObligatorio = REGLAS_DE_OBLIGATORIO.contains(campo.getCode());
            if (esObligatorio || !errores.containsKey(campo.getField())) {
                errores.put(campo.getField(), campo.getDefaultMessage());
            }
        }
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Datos inválidos");
        problema.setDetail("Revisa los campos marcados.");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    /** 400 cuando el cuerpo no es un JSON válido o tiene tipos equivocados. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException error,
                                                                  HttpHeaders encabezados, HttpStatusCode estado,
                                                                  WebRequest solicitud) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Datos inválidos");
        problema.setDetail("El cuerpo de la solicitud no es un JSON válido.");
        return ResponseEntity.badRequest().body(problema);
    }

    /** Pone en español el título de los errores del framework que no personalizamos arriba. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object cuerpo, HttpHeaders encabezados,
                                                             HttpStatusCode estado, WebRequest solicitud) {
        if (cuerpo instanceof ProblemDetail problema && TITULOS.containsKey(estado.value())) {
            HttpStatus estadoConocido = HttpStatus.resolve(estado.value());
            boolean tituloPorDefecto = estadoConocido != null
                    && estadoConocido.getReasonPhrase().equals(problema.getTitle());
            if (tituloPorDefecto) {
                problema.setTitle(TITULOS.get(estado.value()));
                // El detalle por defecto viene en inglés y a veces con datos internos.
                problema.setDetail(null);
            }
        }
        return super.handleExceptionInternal(error, cuerpo, encabezados, estado, solicitud);
    }

    /** 500 para todo lo no previsto. No se le muestra al cliente el mensaje real: puede tener datos internos. */
    @ExceptionHandler(Exception.class)
    ProblemDetail errorInesperado(Exception error) throws Exception {
        // Los errores de seguridad los responde Spring Security con 401 o 403; no los convertimos en 500.
        if (error instanceof AccessDeniedException || error instanceof AuthenticationException) {
            throw error;
        }
        log.error("Error inesperado atendiendo la solicitud", error);
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problema.setTitle(TITULOS.get(500));
        problema.setDetail("Intenta de nuevo en unos minutos.");
        return problema;
    }
}
