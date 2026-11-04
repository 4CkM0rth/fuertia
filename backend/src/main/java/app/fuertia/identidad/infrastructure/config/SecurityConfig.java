package app.fuertia.identidad.infrastructure.config;

import app.fuertia.identidad.infrastructure.persistence.ContextoDeTenant;
import app.fuertia.identidad.infrastructure.web.FiltroDeTenantDelToken;
import app.fuertia.identidad.infrastructure.web.RespuestasDeSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

/**
 * Seguridad HTTP de toda la API.
 *
 * - Sin estado: no hay sesión HTTP en el servidor (el backend corre en varias copias en ECS). Cada
 *   petición trae su JWT en "Authorization: Bearer" y se verifica sola, con la llave pública.
 * - El CSRF de Spring está apagado: el token de acceso va en un encabezado, que el navegador nunca
 *   manda solo. La única cookie (la de renovación) tiene su propia defensa: SameSite=Strict más el
 *   encabezado X-Fuertia-Csrf, que revisa SesionesController.
 * - @EnableMethodSecurity activa @PreAuthorize en los controladores, para pedir un rol concreto.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfig {

    /** Endpoints sin sesión. Lista cerrada, con método y ruta exactos: nada de comodines. */
    private static final RequestMatcher PUBLICAS = new OrRequestMatcher(
            pathPattern(HttpMethod.GET, "/api/v1/sistema/estado"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/registro"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/verificacion"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/sesiones"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/sesiones/renovacion"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/recuperacion"),
            pathPattern(HttpMethod.POST, "/api/v1/identidad/recuperacion/confirmacion"));

    /** Documentación de la API: solo existe y solo es pública en el perfil local. */
    private static final RequestMatcher DOCUMENTACION = new OrRequestMatcher(
            pathPattern("/v3/api-docs"), pathPattern("/v3/api-docs/**"),
            pathPattern("/swagger-ui.html"), pathPattern("/swagger-ui/**"));

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, CorsConfigurationSource fuenteDeCors,
                                          JwtDecoder decodificador, ContextoDeTenant contexto,
                                          Environment entorno) throws Exception {
        boolean perfilLocal = entorno.matchesProfiles("local");
        http
                .csrf(AbstractHttpConfigurer::disable)
                // Se pasa explícita: withDefaults() solo encuentra un bean llamado "corsConfigurationSource"
                // y, si no, usa la configuración vacía de Spring MVC y responde sin encabezados de CORS.
                .cors(cors -> cors.configurationSource(fuenteDeCors))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(RespuestasDeSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasDeSeguridad.sinPermiso()))
                .oauth2ResourceServer(servidor -> servidor
                        .bearerTokenResolver(ignorarTokenEnRutasPublicas())
                        .authenticationEntryPoint(RespuestasDeSeguridad.noAutenticado())
                        .accessDeniedHandler(RespuestasDeSeguridad.sinPermiso())
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(rolesDelToken())))
                // Justo después de verificar el JWT: el tenant queda puesto antes de llegar al controlador.
                .addFilterAfter(new FiltroDeTenantDelToken(contexto), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(rutas -> {
                    rutas.requestMatchers(PUBLICAS).permitAll();
                    // /** cubre las sondas liveness y readiness que usará ECS.
                    rutas.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    // Spring reenvía los errores a /error; si fuera privado, un 400 o 500 de una
                    // ruta pública saldría como 401 y se perdería el mensaje real.
                    rutas.requestMatchers("/error").permitAll();
                    if (perfilLocal) {
                        rutas.requestMatchers(DOCUMENTACION).permitAll();
                    }
                    rutas.anyRequest().authenticated();
                });
        return http.build();
    }

    /**
     * En las rutas públicas se ignora el encabezado Authorization. Si la web manda por costumbre un
     * token de acceso ya vencido al renovar o al iniciar sesión, la petición no debe fallar por eso.
     */
    private static BearerTokenResolver ignorarTokenEnRutasPublicas() {
        DefaultBearerTokenResolver normal = new DefaultBearerTokenResolver();
        return peticion -> PUBLICAS.matches(peticion) ? null : normal.resolve(peticion);
    }

    /** El rol sale del claim "rol" del JWT y se convierte en ROLE_ENTRENADOR, ROLE_ASESORADO o ROLE_ADMIN. */
    private static JwtAuthenticationConverter rolesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(FabricaDeJwt.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    @Bean
    CorsConfigurationSource fuenteDeCors(PropiedadesDeCors propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propiedades.origenesPermitidos());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Fuertia-Csrf"));
        // La web lee Retry-After para decir cuánto esperar después de un 429.
        cors.setExposedHeaders(List.of("Retry-After"));
        // true: la web (localhost:5173) llama a la API (localhost:8080) con la cookie de renovación.
        // Por eso los orígenes son una lista exacta y nunca "*", que con credenciales sería un hueco.
        cors.setAllowCredentials(true);
        cors.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }
}
