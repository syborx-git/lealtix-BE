package com.lealtixservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable());

        // Si falta o es inválido el token, responder 401 (el front invalida sesión y manda a login).
        // Sin esto, Spring devuelve 403 y el panel queda "atorado".
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)));

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.authorizeHttpRequests(authz -> authz
                        // Preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- Recursos estáticos / SPA / docs ---
                        .requestMatchers(
                                "/",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error",
                                "/landing-page/**",
                                "/index.html",
                                "/favicon.ico",
                                "/*.js",
                                "/*.css",
                                "/*.map",
                                "/*.ttf",
                                "/*.eot",
                                "/*.woff*",
                                "/assets/**",
                                "/media/**"
                        ).permitAll()

                        // --- Flujos PÚBLICOS (sin token) ---
                        // Autenticación de usuarios del panel
                        .requestMatchers("/api/tenant/auth/**").permitAll()
                        // Landing / autofactura / menú público
                        .requestMatchers("/api/tenant/slug/**", "/api/tenant/config/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tenant").permitAll()
                        .requestMatchers("/api/tenant-config/**").permitAll()
                        .requestMatchers("/api/tenant-customers/**").permitAll()
                        .requestMatchers("/api/tenant-menu-products/tenant/**").permitAll()
                        .requestMatchers("/api/tenant-menu-products/*/suggestions").permitAll()
                        .requestMatchers("/api/tenant-menu-categories/categories/**").permitAll()
                        .requestMatchers("/api/tenant-menu-categories/catalog/**").permitAll()
                        .requestMatchers("/api/allergies/**").permitAll()
                        // Chatbot de clientes
                        .requestMatchers("/api/chatbot/**").permitAll()
                        // Pagos de clientes (Stripe)
                        .requestMatchers("/api/tenant-payment/**").permitAll()
                        .requestMatchers("/api/stripe/webhook").permitAll()
                        .requestMatchers("/api/stripe/checkout-cancel/**").permitAll()
                        // Redención de cupones / QR
                        .requestMatchers("/api/redemptions/validate/**").permitAll()
                        .requestMatchers("/api/redemptions/redeem/**").permitAll()
                        .requestMatchers("/api/redeem/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/*/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/check/**").permitAll()
                        // Registro / invitaciones
                        .requestMatchers("/api/registro/**").permitAll()
                        .requestMatchers("/api/pre-registro/**").permitAll()
                        .requestMatchers("/api/invitations/validate-token").permitAll()
                        .requestMatchers("/api/appusers/validate/**").permitAll()
                        // Estado de una orden puntual (landing, por UUID)
                        .requestMatchers(HttpMethod.GET, "/api/tenant-client-orders/*").permitAll()
                        // Autofactura desde el landing (el cliente factura su ticket)
                        .requestMatchers(HttpMethod.POST, "/api/facturapi/invoices").permitAll()
                        // Recompensas/campañas mostradas en el landing
                        .requestMatchers(HttpMethod.GET, "/api/campaigns/business/**").permitAll()
                        // Server-Sent Events (el panel los consume sin cabecera Authorization)
                        .requestMatchers("/api/sse/**").permitAll()

                        // --- TODO LO DEMÁS BAJO /api REQUIERE AUTENTICACIÓN ---
                        // (incluye /api/jwt/** que permitía emitir tokens de cualquier tenant)
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
