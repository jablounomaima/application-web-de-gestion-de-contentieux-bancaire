package com.example.contentieux_security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

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
            .authorizeHttpRequests(auth -> auth

                // Pages publiques
                .requestMatchers("/", "/login", "/error",
                        "/css/**", "/js/**", "/images/**")
                .permitAll()

                // Admin
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                // Agent bancaire
                .requestMatchers("/agent/**")
                .hasRole("AGENT")

                // Avocat
                .requestMatchers("/avocat/**")
                .hasRole("AVOCAT")

                // Expert
                .requestMatchers("/expert/**")
                .hasRole("EXPERT")

                // Huissier
                .requestMatchers("/huissier/**")
                .hasRole("HUISSIER")

                // ✅ Validateur financier — URL spécifique en premier
                .requestMatchers("/validateur/financier/**")
                .hasAnyRole("VALIDATEUR_FINANCIER", "ADMIN")

                // ✅ Validateur juridique — URL spécifique en premier
                .requestMatchers("/validateur/juridique/**")
                .hasAnyRole("VALIDATEUR_JURIDIQUE", "ADMIN")

                // ✅ Commun validateurs
                .requestMatchers("/validateur/**")
                .hasAnyRole("VALIDATEUR_FINANCIER", "VALIDATEUR_JURIDIQUE", "ADMIN")


                .requestMatchers("/prestataire/**")
                .hasAnyRole("AVOCAT", "HUISSIER", "EXPERT",
                "VALIDATEUR_JURIDIQUE", "VALIDATEUR_FINANCIER")

                .anyRequest().authenticated()
            )

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(user ->
                    user.oidcUserService(oidcUserService())
                )
                // ✅ Redirection après login selon le rôle
                .successHandler((request, response, authentication) -> {

                    // ← ajoutez ces 4 lignes
                    System.out.println("=== ROLES SPRING ===");
                    authentication.getAuthorities().forEach(a ->
                        System.out.println("  → " + a.getAuthority())
                    );
                    System.out.println("====================");
                
                    if (hasRole(authentication, "ROLE_ADMIN")) {
                        response.sendRedirect("/admin/dashboard");

                    } else if (hasRole(authentication, "ROLE_AGENT")) {
                        response.sendRedirect("/agent/dashboard");

                    } else if (hasRole(authentication, "ROLE_AVOCAT")) {
                        response.sendRedirect("/avocat/dashboard");

                    } else if (hasRole(authentication, "ROLE_HUISSIER")) {
                        response.sendRedirect("/huissier/dashboard");

                    } else if (hasRole(authentication, "ROLE_EXPERT")) {
                        response.sendRedirect("/expert/dashboard");

                    // ✅ Ajout des deux validateurs
                    } else if (hasRole(authentication, "ROLE_VALIDATEUR_JURIDIQUE")) {
                        response.sendRedirect("/validateur/juridique/dashboard");

                    } else if (hasRole(authentication, "ROLE_VALIDATEUR_FINANCIER")) {
                        response.sendRedirect("/validateur/financier/dashboard");

                    } else {
                        // Aucun rôle reconnu → page d'accès refusé
                        response.sendRedirect("/access-denied");
                    }
                })
            )

            .logout(logout ->
                logout.logoutSuccessHandler(keycloakLogoutSuccessHandler())
            );

        return http.build();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    @Bean
    public LogoutSuccessHandler keycloakLogoutSuccessHandler() {

        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) -> {

            String idToken = null;

            if (authentication instanceof OAuth2AuthenticationToken oauthToken
                    && oauthToken.getPrincipal() instanceof OidcUser oidcUser) {
                idToken = oidcUser.getIdToken().getTokenValue();
            }

            String logoutUrl =
                    "http://127.0.0.1:8080/realms/contentieux-realm/protocol/openid-connect/logout"
                    + "?post_logout_redirect_uri=http://localhost:8097";

            if (idToken != null) {
                logoutUrl += "&id_token_hint=" + idToken;
            }

            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.sendRedirect(logoutUrl);
        };
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {

        OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {

            OidcUser oidcUser = delegate.loadUser(userRequest);
            Map<String, Object> claims = oidcUser.getClaims();
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            System.out.println("Claims Keycloak: " + claims);
            System.out.println("Preferred username: " + claims.get("preferred_username"));
            System.out.println("Roles: " + (
                claims.containsKey("realm_access")
                ? ((Map<?,?>) claims.get("realm_access")).get("roles")
                : "aucun"
            ));

            if (claims.containsKey("realm_access")) {

                Map<String, Object> realmAccess =
                        (Map<String, Object>) claims.get("realm_access");

                if (realmAccess.containsKey("roles")) {

                    List<String> roles = (List<String>) realmAccess.get("roles");

                    roles.forEach(role ->
                        mappedAuthorities.add(
                            new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                        )
                    );
                }
            }

            String preferredUsername = (String) claims.get("preferred_username");
            String email = (String) claims.get("email");

            return new DefaultOidcUser(
                    mappedAuthorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo()) {

                @Override
                public String getName() {
                    return preferredUsername != null ? preferredUsername : email;
                }
            };
        };
    }
}