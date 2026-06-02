package com.example.contentieux_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ✅ IMPORTANT : les règles les plus spécifiques en PREMIER
            // Spring Security les évalue dans l'ordre — première correspondance gagne
            .authorizeHttpRequests(auth -> auth

                // ── Routes publiques ──────────────────────────────────
                // ✅ permitAll en premier — pas de token requis
                .requestMatchers(
                    "/api/public/**",
                    "/error",
                    "/ws/**",                  // ✅ WebSocket
                    "/ws-notifications/**",    // ✅ SockJS notifications endpoint
                    "/ws/info/**"              // ✅ SockJS info endpoint
                ).permitAll()

                // ── Admin ─────────────────────────────────────────────
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // ── Agent ─────────────────────────────────────────────
                .requestMatchers("/api/agent/**")
                    .hasAnyRole("AGENT", "ADMIN")

                // ── Avocat ────────────────────────────────────────────
                .requestMatchers("/api/avocat/**")
                    .hasRole("AVOCAT")

                // ── Expert ────────────────────────────────────────────
                .requestMatchers("/api/expert/**")
                    .hasRole("EXPERT")

                // ── Huissier ──────────────────────────────────────────
                .requestMatchers("/api/huissier/**")
                    .hasRole("HUISSIER")

                // ── Validateur — routes spécifiques AVANT génériques ──
                // ✅ Règle spécifique financier — AVANT /api/validateur/**
                .requestMatchers("/api/validateur/financier/**")
                    .hasAnyRole("VALIDATEUR_FINANCIER", "ADMIN")

                // ✅ Règle spécifique juridique — AVANT /api/validateur/**
                .requestMatchers("/api/validateur/juridique/**")
                    .hasAnyRole("VALIDATEUR_JURIDIQUE", "ADMIN")

                // ✅ mon-profil — accessible aux deux types de validateurs
                .requestMatchers("/api/validateur/mon-profil")
                    .hasAnyRole("VALIDATEUR_FINANCIER", "VALIDATEUR_JURIDIQUE")

                // ✅ Règle générique validateur — APRÈS les spécifiques
                .requestMatchers("/api/validateur/**")
                    .hasAnyRole("VALIDATEUR_FINANCIER", "VALIDATEUR_JURIDIQUE", "ADMIN")

                // ── Prestataire ───────────────────────────────────────
                .requestMatchers("/api/prestataire/**")
                    .hasAnyRole("HUISSIER", "EXPERT", "AVOCAT")

                // ── Notifications ─────────────────────────────────────
                // ✅ Accessible à tous les utilisateurs authentifiés
                .requestMatchers("/api/notifications/**")
                    .authenticated()

                // ── Toute autre route ─────────────────────────────────
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))

            // ✅ Pour SockJS — autorise les iframes same-origin
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200"));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // ✅ preferred_username comme principal (pas l'UUID Keycloak)
        converter.setPrincipalClaimName("preferred_username");

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // ✅ Source 1 — realm_access (rôles Realm Keycloak)
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                for (String role : roles) {
                    String roleName = role.toUpperCase();
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    authorities.add(new SimpleGrantedAuthority(roleName));
                }
            }

            // ✅ Source 2 — resource_access (rôles Client Keycloak)
            // Couvre le cas où les rôles sont définis au niveau client
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, clientRoles) -> {
                    if (clientRoles instanceof Map) {
                        Object rolesObj = ((Map<?, ?>) clientRoles).get("roles");
                        if (rolesObj instanceof List) {
                            for (Object role : (List<?>) rolesObj) {
                                String roleName = role.toString().toUpperCase();
                                if (!roleName.startsWith("ROLE_")) {
                                    roleName = "ROLE_" + roleName;
                                }
                                authorities.add(new SimpleGrantedAuthority(roleName));
                            }
                        }
                    }
                });
            }

            return authorities;
        });

        return converter;
    }
}