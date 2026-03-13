package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── Page notifications validateur financier ────────────
    @GetMapping("/validateur/financier/notifications")
    @PreAuthorize("hasRole('VALIDATEUR_FINANCIER')")
    public String notificationsFinancier(Model model, Principal principal) {
        String username = principal.getName();
        List<Notification> notifications = notificationService.getNotifications(username);
        notificationService.marquerToutesLues(username); // marquer lues à l'ouverture
        model.addAttribute("notifications", notifications);
        model.addAttribute("titre", "Mes notifications — Validateur Financier");
        model.addAttribute("retourUrl", "/validateur/financier/dossiers");
        return "validateur/notifications";
    }

    // ── Page notifications validateur juridique ────────────
    @GetMapping("/validateur/juridique/notifications")
    @PreAuthorize("hasRole('VALIDATEUR_JURIDIQUE')")
    public String notificationsJuridique(Model model, Principal principal) {
        String username = principal.getName();
        List<Notification> notifications = notificationService.getNotifications(username);
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
}