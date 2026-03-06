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

    /**
     * Encoder pour crypter les mots de passe
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuration principale de Spring Security
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                /**
                 * Pages publiques accessibles sans authentification
                 */
                .requestMatchers("/", "/login", "/error",
                        "/css/**", "/js/**", "/images/**")
                .permitAll()

                /**
                 * Accès ADMIN uniquement
                 */
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                /**
                 * Accès Agent bancaire
                 */
                .requestMatchers("/agent/**")
                .hasRole("AGENT")

                /**
                 * Accès Avocat
                 */
                .requestMatchers("/avocat/**")
                .hasRole("AVOCAT")

                /**
                 * Accès Expert
                 */
                .requestMatchers("/expert/**")
                .hasRole("EXPERT")

                /**
                 * Accès Huissier
                 */
                .requestMatchers("/huissier/**")
                .hasRole("HUISSIER")

                /**
                 * Accès aux validateurs
                 */
                .requestMatchers("/validateur/**")
                .hasAnyRole(
                        "VALIDATEUR",
                        "VALID_FINANCIER",
                        "VALID_JURIDIQUE",
                        "ADMIN"
                )

                /**
                 * Toutes les autres pages nécessitent une authentification
                 */
                .anyRequest().authenticated()
            )

            /**
             * Configuration login avec OAuth2 (Keycloak)
             */
            .oauth2Login(oauth2 -> oauth2

                /**
                 * Récupération des informations utilisateur depuis Keycloak
                 */
                .userInfoEndpoint(user ->
                        user.oidcUserService(oidcUserService())
                )

                /**
                 * Après login -> redirection selon le rôle
                 */
                .successHandler((request, response, authentication) -> {

                    boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
                    boolean isAgent = hasRole(authentication, "ROLE_AGENT");
                    boolean isAvocat = hasRole(authentication, "ROLE_AVOCAT");
                    boolean isHuissier = hasRole(authentication, "ROLE_HUISSIER");
                    boolean isExpert = hasRole(authentication, "ROLE_EXPERT");

                    if (isAdmin) {
                        response.sendRedirect("/admin/dashboard");
                    }
                    else if (isAgent) {
                        response.sendRedirect("/agent/dashboard");
                    }
                    else if (isAvocat) {
                        response.sendRedirect("/avocat/dashboard");
                    }
                    else if (isHuissier) {
                        response.sendRedirect("/huissier/dashboard");
                    }
                    else if (isExpert) {
                        response.sendRedirect("/expert/dashboard");
                    }
                    else {
                        response.sendRedirect("/");
                    }
                })
            )

            /**
             * Configuration du logout
             */
            .logout(logout ->
                logout.logoutSuccessHandler(keycloakLogoutSuccessHandler())
            );

        return http.build();
    }

    /**
     * Fonction utilitaire pour vérifier si l'utilisateur possède un rôle
     */
    private boolean hasRole(Authentication authentication, String role) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    /**
     * Logout propre vers Keycloak
     */
    @Bean
    public LogoutSuccessHandler keycloakLogoutSuccessHandler() {

        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) -> {

            String idToken = null;

            /**
             * Récupérer le token utilisateur depuis Keycloak
             */
            if (authentication instanceof OAuth2AuthenticationToken oauthToken
                    && oauthToken.getPrincipal() instanceof OidcUser oidcUser) {

                idToken = oidcUser.getIdToken().getTokenValue();
            }

            /**
             * URL de logout Keycloak
             */
            String logoutUrl =
                    "http://127.0.0.1:8080/realms/contentieux-realm/protocol/openid-connect/logout"
                    + "?post_logout_redirect_uri=http://localhost:8097";

            /**
             * Ajouter token pour logout complet
             */
            if (idToken != null) {
                logoutUrl += "&id_token_hint=" + idToken;
            }

            /**
             * Déconnexion Spring Security
             */
            new SecurityContextLogoutHandler()
                    .logout(request, response, authentication);

            /**
             * Redirection vers logout Keycloak
             */
            response.sendRedirect(logoutUrl);
        };
    }

    /**
     * Mapping des rôles Keycloak vers Spring Security
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {

        OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {

            /**
             * Charger l'utilisateur depuis Keycloak
             */
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Map<String, Object> claims = oidcUser.getClaims();
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            /**
             * Debug informations Keycloak
             */
            System.out.println("Claims Keycloak: " + claims);
            System.out.println("Preferred username: " + claims.get("preferred_username"));
            System.out.println("Email: " + claims.get("email"));

            /**
             * Récupérer les rôles dans realm_access.roles
             */
            if (claims.containsKey("realm_access")) {

                Map<String, Object> realmAccess =
                        (Map<String, Object>) claims.get("realm_access");

                if (realmAccess.containsKey("roles")) {

                    List<String> roles =
                            (List<String>) realmAccess.get("roles");

                    /**
                     * Transformer les rôles en ROLE_XXXX
                     */
                    roles.forEach(role ->
                        mappedAuthorities.add(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role.toUpperCase()
                            )
                        )
                    );
                }
            }

            /**
             * récupérer username et email
             */
            String preferredUsername = (String) claims.get("preferred_username");
            String email = (String) claims.get("email");

            /**
             * créer utilisateur Spring Security
             */
            return new DefaultOidcUser(
                    mappedAuthorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo()) {

                /**
                 * définir le nom principal de l'utilisateur
                 */
                @Override
                public String getName() {
                    return preferredUsername != null ? preferredUsername : email;
                }
            };
        };
    }
}