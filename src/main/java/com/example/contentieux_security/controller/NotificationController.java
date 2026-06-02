package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.NotificationDTO;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Convertit une entité Notification en DTO avec dossierId et urlAction */
    private NotificationDTO toDTO(Notification n) {
        Long dossierId = null;
        if (n.getDossier() != null) {
            try {
                dossierId = n.getDossier().getId();
            } catch (Exception ignored) {}
        }

        // Fallback : extraire l'ID depuis urlAction si dossier non chargé
        if (dossierId == null && n.getUrlAction() != null) {
            java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("/dossiers/(\\d+)").matcher(n.getUrlAction());
            if (m.find()) dossierId = Long.parseLong(m.group(1));
        }

        return NotificationDTO.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType())
                .dateCreation(n.getDateCreation() != null ? n.getDateCreation().toString() : null)
                .lue(n.isLue())
                .urlAction(n.getUrlAction())
                .dossierId(dossierId)
                .build();
    }

    // ── GET /api/notifications ───────────────────────────────────
    @GetMapping
    public ResponseEntity<?> mesNotifications(Principal principal,
                                               Authentication authentication) {
        String username = principal.getName();
        log.debug("Notifications demandées par : {}", username);

        List<NotificationDTO> notifications =
            notificationService.getNotifications(username)
                               .stream()
                               .map(this::toDTO)
                               .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("notifications", notifications));
    }

    // ── GET /api/notifications/non-lues ─────────────────────────
    @GetMapping("/non-lues")
    public ResponseEntity<?> nonLues(Principal principal) {
        String username = principal.getName();
        List<NotificationDTO> nonLues = notificationService.getNonLues(username)
                                                            .stream()
                                                            .map(this::toDTO)
                                                            .collect(Collectors.toList());
        long count = notificationService.countNonLues(username);

        return ResponseEntity.ok(Map.of(
            "notifications", nonLues,
            "count",         count
        ));
    }

    // ── GET /api/notifications/count ────────────────────────────
    @GetMapping("/count")
    public ResponseEntity<?> count(Principal principal) {
        long count = notificationService.countNonLues(principal.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ── PUT /api/notifications/{id}/lue ─────────────────────────
    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
        return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
    }

    // ── PUT /api/notifications/toutes-lues ──────────────────────
    @PutMapping("/toutes-lues")
    public ResponseEntity<?> marquerToutesLues(Principal principal) {
        notificationService.marquerToutesLues(principal.getName());
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }
}