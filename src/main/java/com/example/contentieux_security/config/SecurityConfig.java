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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
       return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/login", "/error",
                        "/css/**", "/js/**", "/images/**").permitAll()

                // ADMIN seulement
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")

                .requestMatchers("/agent/**")
                .hasAnyRole("AGENT")

                .requestMatchers("/avocat/**")
                .hasRole("AVOCAT")

                .requestMatchers("/expert/**")
                .hasRole("EXPERT")

                .requestMatchers("/huissier/**")
                .hasRole("HUISSIER")

                .requestMatchers("/validateur/**")
                .hasAnyRole("VALIDATEUR",
                            "VALID_FINANCIER",
                            "VALID_JURIDIQUE",
                            "ADMIN")

                .anyRequest().authenticated()
            )

                .oauth2Login(oauth2 -> oauth2
                .successHandler((request, response, authentication) -> {
                // Vérifier le rôle et rediriger
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                boolean isAgent = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));

                if (isAdmin) {
                    response.sendRedirect("/admin/dashboard");
                } else if (isAgent) {
                    response.sendRedirect("/agent/dashboard");
                } else {
                    response.sendRedirect("/login"); // pas de rôle connu
                }
            })
        )

            .logout(logout -> logout
                .logoutSuccessHandler(keycloakLogoutSuccessHandler())
            );

        return http.build();
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

            new SecurityContextLogoutHandler()
                    .logout(request, response, authentication);

            response.sendRedirect(logoutUrl);
        };
    }

    /**
     * Mapping des rôles Keycloak vers Spring
     */
  
   @Bean
public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
    OidcUserService delegate = new OidcUserService();

    return (userRequest) -> {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Map<String, Object> claims = oidcUser.getClaims();
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        // DEBUG
        System.out.println("Claims Keycloak: " + claims);
        System.out.println("Preferred username: " + claims.get("preferred_username"));
        System.out.println("Email: " + claims.get("email"));
        System.out.println("Sub (ID): " + claims.get("sub"));

        // Mapper les rôles
        if (claims.containsKey("realm_access")) {
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                roles.forEach(role -> mappedAuthorities.add(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                ));
            }
        }

        // IMPORTANT: Créer un OidcUser personnalisé avec le bon username
        String preferredUsername = (String) claims.get("preferred_username");
        String email = (String) claims.get("email");
        
        // Utiliser preferred_username comme principal name
        return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo()) {
            @Override
            public String getName() {
                return preferredUsername != null ? preferredUsername : email;
            }
        };
    };
}
}