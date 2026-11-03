package app.fuertia.identidad.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Seguridad HTTP de toda la API.
 *
 * - Sin estado: no hay sesión en el servidor (el backend corre en varias copias en ECS). La
 *   autenticación con JWT llega en la semana 2; mientras tanto todo lo que no es público responde 401.
 * - CSRF apagado: CSRF ataca a quien se autentica con cookies que el navegador manda solo.
 *   Esta API no usa cookies (el token irá en el encabezado Authorization), así que no aplica.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // Sin esto Spring responde 403 a quien no se autenticó; 401 es lo correcto.
                .exceptionHandling(errores -> errores.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/api/v1/sistema/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/identidad/registro").permitAll()
                        // /** cubre las sondas liveness y readiness que usará ECS.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Spring reenvía los errores a /error; si fuera privado, un 400 o 500 de una
                        // ruta pública saldría como 401 y se perdería el mensaje real.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    CorsConfigurationSource fuenteDeCors(PropiedadesDeCors propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propiedades.origenesPermitidos());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Sin cookies: el token viaja en Authorization, así que no hace falta enviar credenciales.
        cors.setAllowCredentials(false);
        cors.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }
}
