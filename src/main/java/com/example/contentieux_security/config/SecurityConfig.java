package com.example.contentieux_security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // URLs publiques
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // URLs sécurisées
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/agent/**").hasAnyRole("AGENT", "ADMIN")
                .requestMatchers("/validateur/**").hasAnyRole("VALIDATEUR_FINANCIER", "VALIDATEUR_JURIDIQUE", "ADMIN")
                .requestMatchers("/avocat/**").hasRole("AVOCAT")
                .requestMatchers("/expert/**").hasRole("EXPERT")
                .requestMatchers("/huissier/**").hasRole("HUISSIER")
                .requestMatchers("/prestataire/**").hasAnyRole("AVOCAT", "EXPERT", "HUISSIER")
                .anyRequest().authenticated()
            )
            // Login avec Keycloak
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(this.oidcUserService())
                )
            )
            // Déconnexion
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    String logoutUrl = "http://localhost:8080/realms/contentieux-realm/protocol/openid-connect/logout"
                    + "?post_logout_redirect_uri=http://localhost:8097/login"
                    + "&client_id=contentieux-client2";
            
                    response.sendRedirect(logoutUrl);
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Map<String, Object> claims = oidcUser.getClaims();
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            // Mapper les rôles Keycloak
            if (claims.containsKey("realm_access")) {
                Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
                if (realmAccess.containsKey("roles")) {
                    List<String> roles = (List<String>) realmAccess.get("roles");
                    roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                }
            }

            // Si pas de rôle trouvé, on donne ROLE_USER par défaut
            if (mappedAuthorities.isEmpty()) {
                mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}