package com.example.contentieux_security.service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class KeycloakUserService {

    private Keycloak keycloak;
    private String realm;

    // Configuration depuis application.properties
    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String serverUrl;

    @Value("${keycloak.realm:contentieux-realm}")
    private String realmName;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    /**
     * Initialisation après construction du bean
     */
    @PostConstruct
    public void init() {
        this.realm = realmName;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .username(adminUsername)
                .password(adminPassword)
                .clientId("admin-cli")
                .build();
        
        System.out.println("✅ KeycloakUserService initialisé");
        System.out.println("   Server URL: " + serverUrl);
        System.out.println("   Realm: " + realm);
    }

    /**
     * Créer un utilisateur dans Keycloak
     */
    public void createUser(String username, String email, String firstName, 
                          String lastName, String password, String roleName) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);

            Response response = keycloak
                    .realm(realm)
                    .users()
                    .create(user);

            if (response.getStatus() != 201) {
                throw new RuntimeException("Erreur création utilisateur Keycloak: " + response.getStatus());
            }

            String userId = response.getLocation()
                    .getPath()
                    .replaceAll(".*/([^/]+)$", "$1");

            // Mot de passe
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .resetPassword(credential);

            // Rôle
            try {
                RoleRepresentation role = keycloak.realm(realm)
                        .roles()
                        .get(roleName)
                        .toRepresentation();

                keycloak.realm(realm)
                        .users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .add(Collections.singletonList(role));
            } catch (Exception e) {
                System.out.println("Rôle " + roleName + " non trouvé: " + e.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur création utilisateur Keycloak", e);
        }
    }

    /**
     * Supprimer un utilisateur
     */
    public void deleteUser(String username) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .search(username, true);

            if (!users.isEmpty()) {
                String userId = users.get(0).getId();
                keycloak.realm(realm).users().delete(userId);
                System.out.println("✅ Utilisateur supprimé: " + username);
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur suppression: " + e.getMessage());
        }
    }

    /**
     * Changer le mot de passe
     */
    public void changeUserPassword(String username, String newPassword) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .search(username, true);
            
            if (users.isEmpty()) {
                throw new RuntimeException("Utilisateur non trouvé: " + username);
            }
            
            String userId = users.get(0).getId();
            
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);
            
            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .resetPassword(credential);
            
            System.out.println("✅ Mot de passe changé pour: " + username);
            
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
            throw new RuntimeException("Erreur changement mot de passe", e);
        }
    }
}