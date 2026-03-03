package com.example.contentieux_security.service;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeycloakUserDetailsService implements UserDetailsService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.target.realm}")
    private String targetRealm;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        UsersResource usersResource = realmResource.users();
        
        // Rechercher l'utilisateur
        List<UserRepresentation> users = usersResource.searchByUsername(username, true);
        
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("Utilisateur non trouvé: " + username);
        }
        
        UserRepresentation user = users.get(0);
        
        // Vérifier si l'utilisateur est actif
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Utilisateur désactivé: " + username);
        }
        
        // Récupérer les rôles
        List<String> roles = realmResource.users().get(user.getId()).roles()
                .realmLevel().listAll().stream()
                .map(role -> "ROLE_" + role.getName().toUpperCase())
                .collect(Collectors.toList());
        
        if (roles.isEmpty()) {
            roles.add("ROLE_USER");
        }
        
        // Construire UserDetails
        return User.builder()
                .username(user.getUsername())
                .password("") // Mot de passe géré par Keycloak, pas ici
                .disabled(!user.isEnabled())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(roles.toArray(new String[0]))
                .build();
    }
}