package com.example.contentieux_security.service;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.target.realm}")
    private String targetRealm;

    /**
     * Crée un utilisateur Keycloak avec rôle et mot de passe
     */
    public String createUser(String username, String email, String firstName,
                             String lastName, String password, String role) {

        // 🔹 Validation des champs
        if (username == null || username.trim().length() < 3) {
            throw new RuntimeException("Le username doit contenir au moins 3 caractères");
        }

        if (email == null || !email.matches("^[\\w-.]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            throw new RuntimeException("Email invalide : " + email);
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }

        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        UsersResource usersResource = realmResource.users();

        // 🔹 Vérifier si l'utilisateur existe déjà
        List<UserRepresentation> existingUsers = usersResource.search(username, true);
        if (!existingUsers.isEmpty()) {
            throw new RuntimeException("Utilisateur existe déjà: " + username);
        }

        // 🔹 Créer l'utilisateur
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);

        var response = usersResource.create(user);

        // 🔹 Si erreur, afficher message détaillé
        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            throw new RuntimeException("Erreur création utilisateur: "
                    + response.getStatus() + " - " + errorMessage);
        }

        // 🔹 Récupérer l'ID de l'utilisateur
        String userId = response.getLocation().getPath().replaceAll(".*/", "");

        // 🔹 Définir le mot de passe
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        UserResource userResource = usersResource.get(userId);
        userResource.resetPassword(credential);

        // 🔹 Assigner le rôle
        assignRoleToUser(realmResource, userId, role);

        return userId;
    }

    /**
     * Met à jour le mot de passe d’un utilisateur existant
     */
    public void updatePassword(String username, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }

        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> users = usersResource.search(username, true);
        if (users.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé: " + username);
        }

        String userId = users.get(0).getId();
        UserResource userResource = usersResource.get(userId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        userResource.resetPassword(credential);
    }

    /**
     * Assigner un rôle
     */
    private void assignRoleToUser(RealmResource realmResource, String userId, String roleName) {
        try {
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'assignation du rôle '" + roleName + "': " + e.getMessage());
        }
    }

    /**
     * Supprime un utilisateur
     */
    public void deleteUser(String username) {
        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> users = usersResource.search(username, true);
        if (users.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé: " + username);
        }

        usersResource.delete(users.get(0).getId());
    }

    /**
     * Vérifie si un utilisateur existe
     */
    public boolean userExists(String username) {
        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        List<UserRepresentation> users = realmResource.users().search(username, true);
        return !users.isEmpty();
    }
}