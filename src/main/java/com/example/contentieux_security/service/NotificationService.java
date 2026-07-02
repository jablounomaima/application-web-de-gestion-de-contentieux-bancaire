package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Notification;
import com.example.contentieux_security.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher         = eventPublisher;
    }

    // ══════════════════════════════════════════════════════════════
    // Événement Spring — publié après save, envoyé après commit
    // ══════════════════════════════════════════════════════════════

    public record NotificationEvent(Notification notification) {}

    // ══════════════════════════════════════════════════════════════
    // Helper interne
    // ══════════════════════════════════════════════════════════════

  // APRÈS — scinder en deux : la logique privée + le save isolé
private void sauvegarderEtEnvoyer(Notification n) {
    try {
        Notification saved = sauvegarderDansNouvelleTransaction(n);
        log.info(">>> [Notification] id={} destinataire='{}' titre='{}'",
            saved.getId(), saved.getDestinataire(), saved.getTitre());
        eventPublisher.publishEvent(new NotificationEvent(saved));
    } catch (Exception e) {
        // La notification échoue sans polluer la transaction principale
        log.warn("⚠️ Notification non envoyée (transaction indépendante) : {}", e.getMessage());
    }
}

@Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
public Notification sauvegarderDansNouvelleTransaction(Notification n) {
    return notificationRepository.save(n);
}
    // ══════════════════════════════════════════════════════════════
    // Méthodes génériques
    // ══════════════════════════════════════════════════════════════

    private String resolveUrl(String type, DossierContentieux dossier) {
        Long id = dossier != null ? dossier.getId() : null;
        return switch (type) {
            // ← CORRIGER : pointer vers dashboard avec dossierId
            case "VALIDATION_FINANCIERE" -> "/validateur/financier/dashboard?dossierId=" + id;
            case "VALIDATION_JURIDIQUE"  -> "/validateur/juridique/dashboard?dossierId="  + id;
    
            case "RESULTAT_SOUMIS",
                 "RESULTAT_MODIFIE",
                 "PV_SOUMIS",
                 "FACTURE_SOUMISE"        -> "/agent/dossiers/" + id + "/resultats-prestataires";
    
            case "VALIDATION_FINANCIERE_OK",
                 "REJET_FINANCIER",
                 "VALIDATION_JURIDIQUE_OK",
                 "REJET_JURIDIQUE",
                 "NOUVELLE_AUDIENCE",
                 "JUGEMENT_RENDU"         -> "/agent/dossiers/" + id;
    
            case "AUDIENCE_MODIFIEE",
                 "RESULTAT_AVOCAT"        -> "/agent/dossiers/" + id + "/affaire";
    
            case "MISSION_REJETEE",
                 "MISSION_CLOTUREE",
                 "RESOUMISSION"           -> "/prestataire/missions/" + id;
    
            default -> "/agent/dossiers/" + (id != null ? id : "");
        };
    }
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
        sauvegarderEtEnvoyer(n);
    }

    // ══════════════════════════════════════════════════════════════
    // 1. Dossier assigné → notifier les deux validateurs
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void notifierValidateursDossierRecu(DossierContentieux dossier,
                                               String usernameValidateurFinancier,
                                               String usernameValidateurJuridique) {
        String titre   = "Nouveau dossier à valider";
        String message = String.format(
            "Le dossier n°%s vous a été soumis pour validation.",
            dossier.getNumeroDossier());

        notifier(usernameValidateurFinancier, titre, message, "VALIDATION_FINANCIERE", dossier);
        notifier(usernameValidateurJuridique, titre, message, "VALIDATION_JURIDIQUE",  dossier);

        log.info("📨 Dossier {} notifié aux validateurs F={} J={}",
            dossier.getNumeroDossier(), usernameValidateurFinancier, usernameValidateurJuridique);
    }

    // ══════════════════════════════════════════════════════════════
    // 2. Validateur valide/rejette → notifier l'agent
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void notifierAdminValidationFinanciere(DossierContentieux dossier,
                                                   boolean valide,
                                                   String commentaire,
                                                   String usernameAdmin) {
        String type    = valide ? "VALIDATION_FINANCIERE_OK" : "REJET_FINANCIER";
        String titre   = valide ? "✅ Dossier validé financièrement" : "❌ Dossier rejeté financièrement";
        String message = valide
            ? String.format("Le dossier n°%s a été validé par le validateur financier.", dossier.getNumeroDossier())
            : String.format("Le dossier n°%s a été rejeté financièrement. Motif : %s",
                dossier.getNumeroDossier(), commentaire != null ? commentaire : "non précisé");

        notifier(usernameAdmin, titre, message, type, dossier);
        log.info("📨 Agent {} notifié — validation financière dossier {} : {}",
            usernameAdmin, dossier.getNumeroDossier(), valide ? "VALIDÉ" : "REJETÉ");
    }

    @Transactional
    public void notifierAdminValidationJuridique(DossierContentieux dossier,
                                                  boolean valide,
                                                  String commentaire,
                                                  String usernameAdmin) {
        String type    = valide ? "VALIDATION_JURIDIQUE_OK" : "REJET_JURIDIQUE";
        String titre   = valide ? "✅ Dossier validé juridiquement" : "❌ Dossier rejeté juridiquement";
        String message = valide
            ? String.format("Le dossier n°%s a été validé par le validateur juridique.", dossier.getNumeroDossier())
            : String.format("Le dossier n°%s a été rejeté juridiquement. Motif : %s",
                dossier.getNumeroDossier(), commentaire != null ? commentaire : "non précisé");

        notifier(usernameAdmin, titre, message, type, dossier);
        log.info("📨 Agent {} notifié — validation juridique dossier {} : {}",
            usernameAdmin, dossier.getNumeroDossier(), valide ? "VALIDÉ" : "REJETÉ");
    }

    // ══════════════════════════════════════════════════════════════
    // 3. Nouvelle mission → notifier prestataire
    // ══════════════════════════════════════════════════════════════
    @Transactional
public void notifierNouvelleMission(String prestataireUsername,
                                    String numeroMission,
                                    Long missionId,
                                    DossierContentieux dossier) {
    // ← Utiliser notifier() avec dossier au lieu de notifierSansDossier()
    notifier(
        prestataireUsername,
        "📋 Nouvelle mission assignée — " + numeroMission
            + " | Dossier " + (dossier != null ? dossier.getNumeroDossier() : "—"),
        String.format("Une nouvelle mission (%s) vous a été assignée pour le dossier n°%s.",
            numeroMission, dossier != null ? dossier.getNumeroDossier() : "—"),
        "NOUVELLE_MISSION",
        dossier,
        "/avocat/affaires"  // urlAction pour l'avocat
    );

    log.info("📨 Prestataire {} notifié — nouvelle mission {}", prestataireUsername, numeroMission);
}
   
   
    @Transactional
    public void notifierMissionModifiee(String prestataireUsername,
                                        String numeroMission,
                                        Long missionId) {
        notifierSansDossier(
            prestataireUsername,
            "✏️ Mission modifiée",
            String.format("La mission %s a été modifiée par l'administrateur.", numeroMission),
            "MISSION_MODIFIEE",
            "/prestataire/missions/" + missionId);
    }

    // ══════════════════════════════════════════════════════════════
    // 4. Validation/Rejet facture → notifier agent + prestataire
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void notifierDecisionFactureMission(boolean valide,
                                               String commentaire,
                                               String agentUsername,
                                               String prestataireUsername,
                                               String numeroMission,
                                               Long missionId,
                                               DossierContentieux dossier) {
        if (valide) {
            if (agentUsername != null && dossier != null) {
            // APRÈS — URL explicite avec missionId
notifier(agentUsername,
    "✅ Facture validée par le validateur financier",
    "La facture de la mission " + numeroMission + " (dossier "
    + dossier.getNumeroDossier() + ") a été validée par le validateur financier."
    + " Vous pouvez maintenant clôturer la mission.",
    "VALIDATION_FINANCIERE_OK",
    dossier,
    "/agent/dossiers/" + dossier.getId()
        + "/resultats-prestataires?missionId=" + missionId
);
            }
            if (prestataireUsername != null) {
                notifierSansDossier(prestataireUsername,
                    "✅ Votre facture a été validée",
                    "Votre facture pour la mission " + numeroMission
                    + " a été validée par le validateur financier.",
                    "VALIDATION_FINANCIERE_OK",
                    "/prestataire/missions/" + missionId);
            }
        } else {
            if (agentUsername != null && dossier != null) {
                notifier(agentUsername,
                    "❌ Facture rejetée — mission " + numeroMission,
                    "La facture de la mission " + numeroMission
                    + " a été rejetée par le validateur financier."
                    + (commentaire != null && !commentaire.isBlank() ? " Motif : " + commentaire : ""),
                    "REJET_FINANCIER", dossier);
            }
            if (prestataireUsername != null) {
                notifierSansDossier(prestataireUsername,
                    "❌ Votre facture a été rejetée",
                    "Votre facture pour la mission " + numeroMission
                    + " a été rejetée par le validateur financier."
                    + (commentaire != null && !commentaire.isBlank() ? " Motif : " + commentaire : "")
                    + " Merci de corriger et resoumettre vos documents.",
                    "REJET_FINANCIER",
                    "/prestataire/missions/" + missionId);
            }
        }

        log.info("📨 Décision facture mission {} — valide={} → agent={} prestataire={}",
            numeroMission, valide, agentUsername, prestataireUsername);
    }

    @Transactional
    public void notifierMissionCloturee(String prestataireUsername,
                                        String numeroMission,
                                        Long missionId) {
        notifierSansDossier(
            prestataireUsername,
            "🏁 Mission clôturée",
            "La mission " + numeroMission
            + " a été validée et clôturée par l'agent bancaire.",
            "MISSION_CLOTUREE",
            "/prestataire/missions/" + missionId);

        log.info("📨 Prestataire {} notifié — mission {} clôturée", prestataireUsername, numeroMission);
    }

    // ══════════════════════════════════════════════════════════════
    // 5. Résultats soumis → notifier agent (legacy)
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void notifierPvSoumis(Long missionId, Long affaireId, String agentUsername) {
        notifierSansDossier(agentUsername,
            "📄 PV de mission soumis",
            String.format("Le procès-verbal pour l'affaire n°%d a été soumis.", affaireId),
            "PV_SOUMIS",
            "/agent/missions/" + missionId);
    }

    @Transactional
    public void notifierFactureSoumise(Long missionId, Long affaireId,
                                       BigDecimal montant, String agentUsername) {
        notifierSansDossier(agentUsername,
            "🧾 Facture d'honoraires soumise",
            String.format("Une facture de %.3f TND a été soumise pour l'affaire n°%d.", montant, affaireId),
            "FACTURE_SOUMISE",
            "/agent/missions/" + missionId);
    }

    @Transactional
    public void notifierResultatsSoumis(Long missionId, String typePrestataire,
                                         String prestataireNom, Long affaireId,
                                         String agentUsername) {
        notifierSansDossier(agentUsername,
            "📬 Résultats soumis par un prestataire",
            String.format("%s (%s) a soumis ses résultats pour l'affaire n°%d.",
                prestataireNom, typePrestataire, affaireId),
            "RESULTATS_SOUMIS",
            "/agent/missions/" + missionId);
    }

    // ══════════════════════════════════════════════════════════════
    // 6. Audience / Jugement
    // ══════════════════════════════════════════════════════════════
    @Transactional
    public void notifierNouvelleAudience(Long affaireId, String dateAudience,
                                          String tribunal, String agentUsername) {
        notifierSansDossier(agentUsername,
            "🗓️ Nouvelle audience planifiée",
            String.format("Audience planifiée le %s au %s pour l'affaire n°%d.",
                dateAudience, tribunal, affaireId),
            "NOUVELLE_AUDIENCE",
            "/agent/affaires/" + affaireId);
    }

    @Transactional
    public void notifierJugementRendu(Long affaireId, String typeJugement,
                                       String montant, String agentUsername) {
        notifierSansDossier(agentUsername,
            "⚖️ Jugement rendu",
            String.format("Jugement '%s' rendu pour l'affaire n°%d. Montant : %s TND.",
                typeJugement, affaireId, montant != null ? montant : "non spécifié"),
            "JUGEMENT_RENDU",
            "/agent/affaires/" + affaireId);
    }

    @Transactional
    public void notifierAvocat(Long missionId, String avocatUsername,
                                String titre, String message, String type) {
        notifierSansDossier(avocatUsername, titre, message,
            type, "/avocat/missions/" + missionId);
    }

    // ══════════════════════════════════════════════════════════════
    // Lecture
    // ══════════════════════════════════════════════════════════════

    public List<Notification> getNotifications(String username) {
        return notificationRepository.findByDestinataireWithDossier(username);
    }

    public List<Notification> getNonLues(String username) {
        return notificationRepository.findNonLuesWithDossier(username);
    }

    public long countNonLues(String username) {
        return notificationRepository.countByDestinataireAndLueFalse(username);
    }

    // ══════════════════════════════════════════════════════════════
    // Marquer comme lue
    // ══════════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════════
    // Helper URL
    // ══════════════════════════════════════════════════════════════
    

    // ══════════════════════════════════════════════════════════════
// 7. Résultats avocat → notifier l'agent bancaire
// ══════════════════════════════════════════════════════════════

@Transactional
public void notifierAgentAudienceAjoutee(String agentUsername, String avocatNom,
                                          String numeroDossier, Long affaireId,
                                          String dateAudience) {
    notifierSansDossier(agentUsername,
        "📅 Nouvelle audience ajoutée",
        String.format("L'avocat %s a ajouté une audience le %s pour le dossier %s.",
            avocatNom, dateAudience, numeroDossier),
        "NOUVELLE_AUDIENCE",
        "/agent/dossiers/" + /* dossierId */ affaireId + "/affaire");
}

@Transactional
public void notifierAgentResultatAjoute(String agentUsername, String avocatNom,
                                         String numeroDossier, String typeResultat,
                                         Long dossierId) {
    notifierSansDossier(agentUsername,
        "📬 Nouveau résultat — " + typeResultat,
        String.format("L'avocat %s a ajouté un résultat (%s) pour le dossier %s.",
            avocatNom, typeResultat, numeroDossier),
        "RESULTAT_AVOCAT",
        "/agent/dossiers/" + dossierId + "/affaire");
}


// Ajouter juste après la méthode notifier() existante (ligne ~50)

// NotificationService.java — ajouter après la méthode notifier() existante (ligne ~50)
// ══════════════════════════════════════════════════════════════
// Méthodes génériques
// ══════════════════════════════════════════════════════════════

@Transactional
public void notifier(String destinataire, String titre,
                     String message, String type,
                     DossierContentieux dossier) {
    String urlAction = resolveUrl(type, dossier);
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
    sauvegarderEtEnvoyer(n);
}

@Transactional
public void notifier(String destinataire, String titre,
                     String message, String type,
                     DossierContentieux dossier,
                     String urlExplicite) {
    Notification n = Notification.builder()
            .destinataire(destinataire)
            .titre(titre)
            .message(message)
            .type(type)
            .dossier(dossier)
            .dateCreation(LocalDateTime.now())
            .lue(false)
            .urlAction(urlExplicite)
            .build();
    sauvegarderEtEnvoyer(n);
}

}