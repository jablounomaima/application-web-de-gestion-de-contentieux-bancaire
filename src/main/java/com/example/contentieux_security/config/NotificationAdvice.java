package com.example.contentieux_security.config;

import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class NotificationAdvice {

    private final NotificationService notificationService;

    /**
     * Injecte notifCount dans TOUTES les pages automatiquement.
     * Le fragment topbar peut ainsi toujours afficher le badge.
     */
    @ModelAttribute
    public void injecterNotifCount(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) return;

        try {
            String username = authentication.getName();
            long count = notificationService.countNonLues(username);
            model.addAttribute("notifCount", count);
        } catch (Exception e) {
            // En cas d'erreur, on met 0 pour ne pas bloquer la page
            model.addAttribute("notifCount", 0L);
        }
    }
}