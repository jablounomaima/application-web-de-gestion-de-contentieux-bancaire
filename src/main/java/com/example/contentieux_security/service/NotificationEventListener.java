package com.example.contentieux_security.service;

import com.example.contentieux_security.dto.NotificationDTO;
import com.example.contentieux_security.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * S'exécute APRÈS le commit de la transaction.
     * Garantit que :
     *  1. La notification est persistée en base
     *  2. La session WebSocket est stable et prête à recevoir
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void envoyerNotificationWebSocket(
            NotificationService.NotificationEvent event) {

        var n = event.notification();
        String dest = n.getDestinataire();

        log.info(">>> [WS AFTER_COMMIT] ========================================");
        log.info(">>> [WS AFTER_COMMIT] Notification reçue");
        log.info(">>> [WS AFTER_COMMIT]   destinataire: '{}'", dest);
        log.info(">>> [WS AFTER_COMMIT]   type: '{}'", n.getType());
        log.info(">>> [WS AFTER_COMMIT]   titre: '{}'", n.getTitre());
        log.info(">>> [WS AFTER_COMMIT]   dossierId: {}", n.getDossier() != null ? n.getDossier().getId() : "null");
        log.info(">>> [WS AFTER_COMMIT] ========================================");

        try {
            // Construire un DTO pour envoyer dossierId + urlAction au frontend
            NotificationDTO dto = toDTO(n);

            log.info(">>> [WS AFTER_COMMIT] Envoi via convertAndSendToUser('/user/{}/queue/notifications')", dest);
            messagingTemplate.convertAndSendToUser(
                dest, "/queue/notifications", dto);
            log.info("✅ [WS AFTER_COMMIT] Notification livrée à '{}'", dest);
        } catch (Exception e) {
            log.error("❌ [WS AFTER_COMMIT] Échec pour '{}': {}", dest, e.getMessage(), e);
        }
    }

    private NotificationDTO toDTO(Notification n) {
        Long dossierId = null;

        // Récupérer le dossierId depuis la relation (chargée avant le commit)
        try {
            if (n.getDossier() != null) {
                dossierId = n.getDossier().getId();
            }
        } catch (Exception ignored) {}

        // Fallback : extraire l'ID depuis urlAction
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
}