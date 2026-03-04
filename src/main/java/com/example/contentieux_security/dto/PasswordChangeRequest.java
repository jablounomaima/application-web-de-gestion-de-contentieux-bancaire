package com.example.contentieux_security.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    private String currentPassword;  // Mot de passe actuel
    private String newPassword;      // Nouveau mot de passe
    private String confirmPassword;  // Confirmation
}