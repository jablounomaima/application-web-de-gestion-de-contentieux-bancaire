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

    // ── Créer une notification pour un validateur ──────────
    @Transactional
    public void notifier(String destinataire, String titre,
                          String message, String type,
                          DossierContentieux dossier) {
        Notification n = Notification.builder()
                .destinataire(destinataire)
                .titre(titre)
                .message(message)
                .type(type)
                .dossier(dossier)
                .dateCreation(LocalDateTime.now())
                .lue(false)
                .urlAction("/validateur/" +
                        (type.equals("VALIDATION_FINANCIERE") ? "financier" : "juridique")
                        + "/dossiers/" + dossier.getId())
                .build();
        notificationRepository.save(n);
    }

    // ── Lecture ────────────────────────────────────────────
    public List<Notification> getNotifications(String username) {
        return notificationRepository
                .findByDestinataire_OrderByDateCreationDesc(username);
    }

    public List<Notification> getNonLues(String username) {
        return notificationRepository
                .findByDestinataire_AndLueFalseOrderByDateCreationDesc(username);
    }

    public long countNonLues(String username) {
        return notificationRepository.countByDestinataire_AndLueFalse(username);
    }

    // ── Marquer comme lue ──────────────────────────────────
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