package app.fuertia.sistema.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * Configuración del almacenamiento de archivos (fotos de comida y PDF), compatible con S3.
 *
 * Local apunta a RustFS (docker-compose.yml). En AWS el código es el mismo y solo cambian las
 * variables de entorno: endpoint vacío (se usa el de S3), path-style en false y sin claves, porque
 * la tarea de ECS se autentica con su rol IAM.
 *
 * Vive en el módulo sistema porque la van a usar varios módulos (nutrición, exportación PDF).
 * @Validated hace que la app no arranque si falta un dato obligatorio: mejor fallar al inicio que
 * al primer intento de subir una foto.
 */
@Validated
@ConfigurationProperties(prefix = "fuertia.almacenamiento")
public record PropiedadesDeAlmacenamiento(
        URI endpoint,
        @NotBlank String region,
        boolean pathStyle,
        String claveDeAcceso,
        String claveSecreta,
        @Valid @NotNull Buckets buckets) {

    public record Buckets(@NotBlank String fotosComida, @NotBlank String pdf) {
    }

    // La clave secreta no debe aparecer si alguien imprime la configuración.
    @Override
    public String toString() {
        return "PropiedadesDeAlmacenamiento[endpoint=" + endpoint + ", region=" + region + ", pathStyle=" + pathStyle
                + ", claveDeAcceso=" + claveDeAcceso + ", claveSecreta=****, buckets=" + buckets + "]";
    }
}
