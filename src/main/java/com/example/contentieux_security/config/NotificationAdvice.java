package com.example.contentieux_security.config;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;  // ✅ JAKARTA (pas javax)
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
    
    // Clé pour le cache de requête
    private static final String NOTIF_COUNT_CACHE_KEY = "NOTIF_COUNT_CACHE";

    @ModelAttribute
    public void injecterNotifCount(Authentication authentication, 
                                   Model model,
                                   HttpServletRequest request) {  // ✅ JAKARTA
        // Si pas authentifié, mettre 0
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("notifCount", 0L);
            return;
        }

        String username = authentication.getName();
        
        // ✅ CACHE : Vérifier si déjà calculé dans cette requête HTTP
        Object cached = request.getAttribute(NOTIF_COUNT_CACHE_KEY);
        if (cached != null) {
            model.addAttribute("notifCount", cached);
            return;
        }

        try {
            long count = notificationService.countNonLues(username);
            request.setAttribute(NOTIF_COUNT_CACHE_KEY, count); // Stocker en cache
            model.addAttribute("notifCount", count);
            log.debug("NotifCount calculé pour {}: {}", username, count);
        } catch (Exception e) {
            log.error("Erreur comptage notifications pour {}", username, e);
            model.addAttribute("notifCount", 0L);
        }
    }

    public void notifier(String username, String string, String messageNotification, String string2,
            DossierContentieux dossier) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notifier'");
    }
}