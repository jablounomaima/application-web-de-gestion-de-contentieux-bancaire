package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Page notifications validateur financier ────────────
    @GetMapping("/validateur/financier/notifications")
    public String notificationsFinancier(Model model, 
                                          Principal principal,
                                          Authentication authentication) {
        logRoles(authentication);
        return afficherNotifications(model, principal.getName(), 
                                     "Mes notifications — Validateur Financier",
                                     "/validateur/financier/dossiers");
    }

    // ── Page notifications validateur juridique ────────────
    @GetMapping("/validateur/juridique/notifications")
    public String notificationsJuridique(Model model, 
                                          Principal principal,
                                          Authentication authentication) {
        logRoles(authentication);
        return afficherNotifications(model, principal.getName(),
                                     "Mes notifications — Validateur Juridique", 
                                     "/validateur/juridique/dossiers");
    }

    // ── Page notifications agent ───────────────────────────
    @GetMapping("/agent/notifications")
    public String notificationsAgent(Model model, 
                                      Principal principal) {
        return afficherNotifications(model, principal.getName(),
                                     "Mes notifications — Agent Bancaire",
                                     "/agent/dossiers");
    }

    // ── Méthode commune factorisée ─────────────────────────
    private String afficherNotifications(Model model, 
                                          String username,
                                          String titre, 
                                          String retourUrl) {
        log.debug("=== Notifications pour username : {}", username);

        List<Notification> notifications = notificationService.getNotifications(username);
        log.debug("=== Nombre de notifications trouvées : {}", notifications.size());
        
        notifications.forEach(n -> log.debug(
                "  → destinataire={} | titre={} | lue={}",
                n.getDestinataire(), n.getTitre(), n.isLue()));

        notificationService.marquerToutesLues(username);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("titre", titre);
        model.addAttribute("retourUrl", retourUrl);
        
        // ✅ PAS DE notifCount ICI - il est déjà injecté par NotificationAdvice
        
        return "validateur/notifications";
    }

    // ── Marquer une notification comme lue ─────────────────
    @PostMapping("/notifications/{id}/lue")
    public String marquerLue(@PathVariable Long id,
                              @RequestHeader(value = "Referer", required = false) String referer) {
        notificationService.marquerLue(id);
        return referer != null ? "redirect:" + referer : "redirect:/";
    }

    // ── Utilitaire ─────────────────────────────────────────
    private void logRoles(Authentication authentication) {
        log.debug("=== ROLES utilisateur : ");
        authentication.getAuthorities()
                .forEach(a -> log.debug("  → {}", a.getAuthority()));
    }
}