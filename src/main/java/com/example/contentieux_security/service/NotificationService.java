package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ── Créer une notification ─────────────────────────────────────────────
    @Transactional
    public void notifier(String destinataire, String titre,
                          String message, String type,
                          DossierContentieux dossier) {
        String urlAction = switch (type) {
            case "VALIDATION_FINANCIERE"    -> "/validateur/financier/dossiers/" + dossier.getId();
            case "VALIDATION_JURIDIQUE"     -> "/validateur/juridique/dossiers/" + dossier.getId();
            case "VALIDATION_FINANCIERE_OK",
                 "REJET_FINANCIER",
                 "VALIDATION_JURIDIQUE_OK",
                 "REJET_JURIDIQUE"          -> "/agent/dossiers/" + dossier.getId();
            default                         -> "/agent/dossiers/" + dossier.getId();
        };
    
        Notification n = Notification.builder()
                .destinataire(destinataire)
                .titre(titre)
                .message(message)
                .type(type)
                .dossier(dossier)
                .dateCreation(LocalDateTime.now())
                .lue(false)
                .urlAction(urlAction)
                .build();
        notificationRepository.save(n);
    }

    // ── Lecture ────────────────────────────────────────────────────────────
    public List<Notification> getNotifications(String username) {
        return notificationRepository
                .findByDestinataireOrderByDateCreationDesc(username); // ✅ sans _
    }

    public List<Notification> getNonLues(String username) {
        return notificationRepository
                .findByDestinataireAndLueFalseOrderByDateCreationDesc(username); // ✅ sans _
    }

    public long countNonLues(String username) {
        return notificationRepository
                .countByDestinataireAndLueFalse(username); // ✅ sans _
    }

    // ── Marquer comme lue ──────────────────────────────────────────────────
    @Transactional
    public void marquerLue(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setLue(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void marquerToutesLues(String username) {
        notificationRepository.marquerToutesLues(username);
    }
}