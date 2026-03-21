package com.lealtixservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centraliza la configuración CORS para la aplicación.
 * Lee los orígenes permitidos desde la propiedad `cors.allowed-origins` en application.properties.
 * Registra configuraciones tanto para WebMvc (controladores) como un CorsFilter global.
 */
@Configuration
public class CorsConfig {

    // Inyecta la lista de orígenes desde application.properties (comma-separated)
    @Value("${cors.allowed-origins:https://admin.lealtix.com.mx,https://lealtix.com.mx,https://www.lealtix.com.mx}")
    private String allowedOriginsProp;

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOriginsProp.split("\\s*,\\s*"))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        List<String> origins = parseAllowedOrigins();
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Mantener mappings existentes y añadir/usar la lista de orígenes desde properties
                registry.addMapping("/api/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Content-Type")
                        .allowCredentials(true)
                        .maxAge(3600);

                // SSE endpoint requiere allowCredentials y cache control propio
                registry.addMapping("/api/sse/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Type", "Cache-Control", "X-Accel-Buffering")
                        .allowCredentials(true)
                        .maxAge(0);

                registry.addMapping("/stripe/**")
                        .allowedOrigins(origins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Type")
                        .allowCredentials(false)
                        .maxAge(3600);

                // No se registra mapping específico para /dashboard/** porque el proxy fue removido
                // Si en el futuro se expone una API para dashboard, registra aquí el mapping adecuado.
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = parseAllowedOrigins();

        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(origins);
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("*"));
        apiConfig.setExposedHeaders(List.of("Authorization", "Content-Type"));
        apiConfig.setAllowCredentials(true);
        apiConfig.setMaxAge(3600L);

        CorsConfiguration sseConfig = new CorsConfiguration();
        sseConfig.setAllowedOrigins(origins);
        sseConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        sseConfig.setAllowedHeaders(List.of("*"));
        sseConfig.setExposedHeaders(List.of("Content-Type", "Cache-Control", "X-Accel-Buffering"));
        sseConfig.setAllowCredentials(true);
        sseConfig.setMaxAge(0L);

        CorsConfiguration stripeConfig = new CorsConfiguration();
        stripeConfig.setAllowedOrigins(origins);
        stripeConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        stripeConfig.setAllowedHeaders(List.of("*"));
        stripeConfig.setExposedHeaders(List.of("Content-Type"));
        stripeConfig.setAllowCredentials(false);
        stripeConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/sse/**", sseConfig);
        source.registerCorsConfiguration("/api/**", apiConfig);
        source.registerCorsConfiguration("/stripe/**", stripeConfig);

        // No se registra configuración especial para /dashboard/** (proxy eliminado)

        return source;
    }

    // También registrar un filtro global CORS por si acaso algún componente no respeta CorsConfigurationSource
    @Bean
    public CorsFilter corsFilter() {
        List<String> origins = parseAllowedOrigins();
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}
