package com.example.contentieux_security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    private String currentPassword;  // Mot de passe actuel
    private String newPassword;      // Nouveau mot de passe
    private String confirmPassword;  // Confirmation
}

