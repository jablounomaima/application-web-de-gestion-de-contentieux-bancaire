package com.example.contentieux_security.config;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class NotificationAdvice {

    private final NotificationService notificationService;

    // Clé de cache par requête pour éviter plusieurs appels SQL sur la même requête HTTP
    private static final String CACHE_KEY = "NOTIF_COUNT_CACHE";

    /**
     * Injecte le nombre de notifications non lues dans chaque vue Thymeleaf.
     * Résultat mis en cache au niveau de la requête HTTP.
     */
    @ModelAttribute
    public void injecterNotifCount(Authentication authentication,
                                    Model model,
                                    HttpServletRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("notifCount", 0L);
            return;
        }

        // Cache par requête — évite N appels si plusieurs @ModelAttribute s'enchaînent
        Object cached = request.getAttribute(CACHE_KEY);
        if (cached != null) {
            model.addAttribute("notifCount", cached);
            return;
        }

        try {
            long count = notificationService.countNonLues(authentication.getName());
            request.setAttribute(CACHE_KEY, count);
            model.addAttribute("notifCount", count);
        } catch (Exception e) {
            log.error("Erreur comptage notifications pour {}",
                authentication.getName(), e);
            model.addAttribute("notifCount", 0L);
        }
    }

    /**
     * Délègue l'envoi d'une notification au NotificationService.
     * Utilisé par les controllers qui injectent NotificationAdvice.
     */
    public void notifier(String username,
                         String titre,
                         String message,
                         String type,
                         DossierContentieux dossier) {
        try {
            notificationService.notifier(username, titre, message, type, dossier);
            log.debug("Notification envoyée → {} | type={} | titre={}",
                username, type, titre);
        } catch (Exception e) {
            log.error("Erreur envoi notification → {}", username, e);
        }
    }
}