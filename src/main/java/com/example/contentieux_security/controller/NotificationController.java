package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> mesNotifications(Principal principal, Authentication authentication) {
        logRoles(authentication);
        String username = principal.getName();
        
        log.debug("=== Notifications pour username : {}", username);

        List<Notification> notifications = notificationService.getNotifications(username);
        log.debug("=== Nombre de notifications trouvées : {}", notifications.size());

        notifications.forEach(n -> log.debug(
                "  → destinataire={} | titre={} | lue={}",
                n.getDestinataire(), n.getTitre(), n.isLue()));

        notificationService.marquerToutesLues(username);

        return ResponseEntity.ok(Map.of("notifications", notifications));
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
        return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
    }

    private void logRoles(Authentication authentication) {
        log.debug("=== ROLES utilisateur : ");
        if (authentication != null && authentication.getAuthorities() != null) {
            authentication.getAuthorities()
                    .forEach(a -> log.debug("  → {}", a.getAuthority()));
        }
    }
}