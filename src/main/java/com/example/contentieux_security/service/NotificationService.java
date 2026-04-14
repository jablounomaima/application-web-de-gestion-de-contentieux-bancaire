package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ── Créer une notification (générique) ─────────────────────────────────
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
            case "NOUVELLE_AUDIENCE"        -> "/agent/affaires/" + (dossier != null ? dossier.getId() : "");
            case "JUGEMENT_RENDU"           -> "/agent/affaires/" + (dossier != null ? dossier.getId() : "");
            case "PV_SOUMIS"                -> "/agent/missions/";
            case "FACTURE_SOUMISE"          -> "/agent/missions/";
            default                         -> "/agent/dossiers/" + (dossier != null ? dossier.getId() : "");
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
        log.info("Notification créée pour {} : {}", destinataire, titre);
    }

    // ── Créer une notification sans dossier ────────────────────────────────
    @Transactional
    public void notifierSansDossier(String destinataire, String titre,
                                      String message, String type,
                                      String urlAction) {
        Notification n = Notification.builder()
                .destinataire(destinataire)
                .titre(titre)
                .message(message)
                .type(type)
                .dossier(null)
                .dateCreation(LocalDateTime.now())
                .lue(false)
                .urlAction(urlAction)
                .build();
        notificationRepository.save(n);
        log.info("Notification (sans dossier) créée pour {} : {}", destinataire, titre);
    }

    // ── NOTIFICATIONS SPÉCIFIQUES POUR L'AVOCAT ────────────────────────────

    /**
     * Notifier l'agent qu'une nouvelle audience a été planifiée
     */
    @Transactional
    public void notifierNouvelleAudience(Long affaireId, String dateAudience, String tribunal) {
        String titre = "Nouvelle audience planifiée";
        String message = String.format("Une audience a été planifiée au %s au tribunal de %s pour l'affaire n°%d",
                dateAudience, tribunal, affaireId);
        String urlAction = "/agent/affaires/" + affaireId;
        
        // Note: l'agentUsername devrait être passé en paramètre ou récupéré depuis l'affaire
        // Pour l'instant, nous laissons le destinataire à déterminer par l'appelant
        log.info("Nouvelle audience notifiée pour affaire {}", affaireId);
    }
    
    // Version avec destinataire explicite
    @Transactional
    public void notifierNouvelleAudience(Long affaireId, String dateAudience, String tribunal, String agentUsername) {
        String titre = "Nouvelle audience planifiée";
        String message = String.format("Une audience a été planifiée au %s au tribunal de %s pour l'affaire n°%d",
                dateAudience, tribunal, affaireId);
        String urlAction = "/agent/affaires/" + affaireId;
        
        notifierSansDossier(agentUsername, titre, message, "NOUVELLE_AUDIENCE", urlAction);
    }

    /**
     * Notifier l'agent qu'un jugement a été rendu
     */
    @Transactional
    public void notifierJugementRendu(Long affaireId, String typeJugement, String montant, String agentUsername) {
        String titre = "Jugement rendu";
        String message = String.format("Un jugement de type '%s' a été rendu pour l'affaire n°%d. Montant: %s TND",
                typeJugement, affaireId, montant != null ? montant : "non spécifié");
        String urlAction = "/agent/affaires/" + affaireId;
        
        notifierSansDossier(agentUsername, titre, message, "JUGEMENT_RENDU", urlAction);
    }

    /**
     * Notifier l'agent qu'un PV de mission a été soumis
     */
    @Transactional
    public void notifierPvSoumis(Long missionId, Long affaireId, String agentUsername) {
        String titre = "PV de mission soumis";
        String message = String.format("Le PV de mission pour l'affaire n°%d a été soumis par l'avocat.", affaireId);
        String urlAction = "/agent/missions/" + missionId;
        
        notifierSansDossier(agentUsername, titre, message, "PV_SOUMIS", urlAction);
    }

    /**
     * Notifier l'agent qu'une facture d'honoraires a été soumise
     */
    @Transactional
    public void notifierFactureSoumise(Long missionId, Long affaireId, BigDecimal montant, String agentUsername) {
        String titre = "Facture d'honoraires soumise";
        String message = String.format("Une facture d'honoraires de %.2f TND a été soumise pour l'affaire n°%d.",
                montant, affaireId);
        String urlAction = "/agent/missions/" + missionId;
        
        notifierSansDossier(agentUsername, titre, message, "FACTURE_SOUMISE", urlAction);
    }

    /**
     * Notifier l'avocat d'une validation ou d'un rejet
     */
    @Transactional
    public void notifierAvocat(Long missionId, String avocatUsername, String titre, String message, String type) {
        String urlAction = "/avocat/missions/" + missionId;
        notifierSansDossier(avocatUsername, titre, message, type, urlAction);
    }

    // ── NOTIFICATIONS POUR LE PRESTATAIRE ───────────────────────────────────

    /**
     * Notifier le prestataire qu'une mission lui a été assignée
     */
    @Transactional
    public void notifierNouvelleMission(String prestataireUsername, String numeroMission, Long missionId) {
        String titre = "Nouvelle mission assignée";
        String message = String.format("Une nouvelle mission (%s) vous a été assignée.", numeroMission);
        String urlAction = "/prestataire/missions/" + missionId;
        
        notifierSansDossier(prestataireUsername, titre, message, "NOUVELLE_MISSION", urlAction);
    }

    /**
     * Notifier le prestataire qu'une mission a été modifiée
     */
    @Transactional
    public void notifierMissionModifiee(String prestataireUsername, String numeroMission, Long missionId) {
        String titre = "Mission modifiée";
        String message = String.format("La mission %s a été modifiée par l'agent.", numeroMission);
        String urlAction = "/prestataire/missions/" + missionId;
        
        notifierSansDossier(prestataireUsername, titre, message, "MISSION_MODIFIEE", urlAction);
    }

    // ── Lecture des notifications ──────────────────────────────────────────
    
    public List<Notification> getNotifications(String username) {
        return notificationRepository.findByDestinataireOrderByDateCreationDesc(username);
    }

    public List<Notification> getNonLues(String username) {
        return notificationRepository.findByDestinataireAndLueFalseOrderByDateCreationDesc(username);
    }

    public long countNonLues(String username) {
        return notificationRepository.countByDestinataireAndLueFalse(username);
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