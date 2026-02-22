package com.example.contentieux_security.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;

import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import com.example.contentieux_security.service.AuthService;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthService authService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/public/**", "/api/auth/login",
                                "/h2-console/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable())) // For H2 console
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        // Combiner Keycloak et Local
        String issuerUri = "http://localhost:8080/realms/contentieux-realm";
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder keycloakDecoder = org.springframework.security.oauth2.jwt.JwtDecoders
                .fromIssuerLocation(issuerUri);

        org.springframework.security.oauth2.jwt.NimbusJwtDecoder localDecoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withSecretKey((javax.crypto.SecretKey) authService.getSigningKey()).build();

        return token -> {
            try {
                return keycloakDecoder.decode(token);
            } catch (Exception e) {
                return localDecoder.decode(token);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Check realm_access (Keycloak) or just custom claims (Local)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles;
            if (realmAccess != null && realmAccess.get("roles") != null) {
                roles = (List<String>) realmAccess.get("roles");
            } else {
                // Local token might have roles directly or in claims
                roles = jwt.getClaim("roles");
                if (roles == null)
                    roles = Collections.emptyList();
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
