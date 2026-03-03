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
                .hasAnyRole("AGENT", "ADMIN")

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
                .defaultSuccessUrl("/admin/dashboard", true)
                .userInfoEndpoint(userInfo ->
                        userInfo.oidcUserService(oidcUserService())
                )
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

        return userRequest -> {

            OidcUser oidcUser =
                    delegate.loadUser(userRequest);

            Map<String, Object> claims =
                    oidcUser.getClaims();

            Set<GrantedAuthority> authorities =
                    new HashSet<>();

            Map<String, Object> realmAccess =
                    (Map<String, Object>) claims.get("realm_access");

            if (realmAccess != null) {

                List<String> roles =
                        (List<String>) realmAccess.get("roles");

                if (roles != null) {
                    roles.forEach(role ->
                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );
                }
            }

            return new DefaultOidcUser(
                    authorities,
                    oidcUser.getIdToken(),
                    oidcUser.getUserInfo()
            );
        };
    }
}