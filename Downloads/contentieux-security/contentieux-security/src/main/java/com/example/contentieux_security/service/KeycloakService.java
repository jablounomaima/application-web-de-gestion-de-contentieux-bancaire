package com.example.contentieux_security.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Collections;

@Service
public class KeycloakService {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String adminRealm;

    @Value("${keycloak.admin.username}")
    private String username;

    @Value("${keycloak.admin.password}")
    private String password;

    @Value("${keycloak.admin.clientId}")
    private String clientId;

    @Value("${keycloak.target.realm}")
    private String targetRealm;

    public void createKeycloakUser(String userUsername, String userPassword, String role) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .build();

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(userUsername);
        user.setFirstName(userUsername);
        user.setEmail(userUsername + "@example.com");

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(userPassword);
        user.setCredentials(Collections.singletonList(passwordCred));

        UsersResource usersResource = keycloak.realm(targetRealm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            // Utilisateur créé -> Assigner le rôle
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            try {
                org.keycloak.representations.idm.RoleRepresentation roleRep = keycloak.realm(targetRealm).roles()
                        .get(role)
                        .toRepresentation();
                usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(roleRep));
            } catch (Exception e) {
                System.out.println("Avertissement: Impossible d'assigner le rôle '" + role + "': " + e.getMessage());
            }
        } else if (response.getStatus() != 409) {
            String error = response.readEntity(String.class);
            throw new RuntimeException("Erreur Keycloak (" + response.getStatus() + "): " + error);
        }
    }

    public void deleteKeycloakUser(String userUsername) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .build();

        UsersResource usersResource = keycloak.realm(targetRealm).users();
        java.util.List<UserRepresentation> foundUsers = usersResource.searchByUsername(userUsername, true);
        for (UserRepresentation user : foundUsers) {
            usersResource.get(user.getId()).remove();
        }
    }

    public void updateKeycloakPassword(String userUsername, String newPassword) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .build();

        UsersResource usersResource = keycloak.realm(targetRealm).users();
        java.util.List<UserRepresentation> foundUsers = usersResource.searchByUsername(userUsername, true);
        for (UserRepresentation user : foundUsers) {
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(newPassword);
            usersResource.get(user.getId()).resetPassword(passwordCred);
        }
    }
}
