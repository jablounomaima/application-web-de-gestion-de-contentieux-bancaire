package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Page notifications validateur financier ────────────
    @GetMapping("/validateur/financier/notifications")
    public String notificationsFinancier(Model model, Principal principal,
                                          Authentication authentication) {
        System.out.println("=== ROLES utilisateur : ");
        authentication.getAuthorities()
                .forEach(a -> System.out.println("  → " + a.getAuthority()));

        String username = principal.getName();
        System.out.println("=== Notifications pour username : " + username);

        List<Notification> notifications = notificationService.getNotifications(username);
        System.out.println("=== Nombre de notifications trouvées : " + notifications.size());
        notifications.forEach(n -> System.out.println(
                "  → destinataire=" + n.getDestinataire()
                + " | titre=" + n.getTitre()
                + " | lue=" + n.isLue()));

        notificationService.marquerToutesLues(username);
        model.addAttribute("notifications", notifications);
        model.addAttribute("titre", "Mes notifications — Validateur Financier");
        model.addAttribute("retourUrl", "/validateur/financier/dossiers");
        return "validateur/notifications";
    }

    // ── Page notifications validateur juridique ────────────
    @GetMapping("/validateur/juridique/notifications")
    public String notificationsJuridique(Model model, Principal principal,
                                          Authentication authentication) {
        System.out.println("=== ROLES utilisateur : ");
        authentication.getAuthorities()
                .forEach(a -> System.out.println("  → " + a.getAuthority()));

        String username = principal.getName();
        System.out.println("=== Notifications pour username : " + username);

        List<Notification> notifications = notificationService.getNotifications(username);
        System.out.println("=== Nombre de notifications trouvées : " + notifications.size());
        notifications.forEach(n -> System.out.println(
                "  → destinataire=" + n.getDestinataire()
                + " | titre=" + n.getTitre()
                + " | lue=" + n.isLue()));

        notificationService.marquerToutesLues(username);
        model.addAttribute("notifications", notifications);
        model.addAttribute("titre", "Mes notifications — Validateur Juridique");
        model.addAttribute("retourUrl", "/validateur/juridique/dossiers");
        return "validateur/notifications";
    }

    // ── Marquer une notification comme lue ─────────────────
    @PostMapping("/notifications/{id}/lue")
    public String marquerLue(@PathVariable Long id,
                              @RequestHeader(value = "Referer", required = false) String referer) {
        notificationService.marquerLue(id);
        return referer != null ? "redirect:" + referer : "redirect:/";
    }




    // ── Page notifications agent ───────────────────────────
@GetMapping("/agent/notifications")
public String notificationsAgent(Model model, Principal principal,
                                  Authentication authentication) {
    String username = principal.getName();
    List<Notification> notifications = notificationService.getNotifications(username);
    notificationService.marquerToutesLues(username);
    model.addAttribute("notifications", notifications);
    model.addAttribute("titre", "Mes notifications — Agent Bancaire");
    model.addAttribute("retourUrl", "/agent/dossiers");
    return "validateur/notifications"; // ← réutilise le même template
}
}