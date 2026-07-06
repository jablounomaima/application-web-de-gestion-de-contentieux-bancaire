package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.CompteBancaireAgence;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.ModePaiement;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║                      MissionService                             ║
 * ╠══════════════════════════════════════════════════════════════════╣
 * ║  Gère le cycle de vie complet d'une mission :                   ║
 * ║                                                                  ║
 * ║  ASSIGNEE → EN_COURS → PV_SOUMIS → FACTURE_SOUMISE             ║
 * ║                                          ↓                      ║
 * ║                             Validateur financier examine        ║
 * ║                            ↙                        ↘          ║
 * ║                   FACTURE_VALIDEE           FACTURE_REJETEE     ║
 * ║                          ↓                       ↓             ║
 * ║                   Agent examine         Prestataire resoumet    ║
 * ║                  ↙          ↘           la facture             ║
 * ║              TERMINEE      REJETEE                              ║
 * ║                               ↓                                 ║
 * ║                    Prestataire resoumet PV + facture            ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final NotificationService notificationService;
    // ══════════════════════════════════════════════════════════════
    //  LECTURE / RECHERCHE
    //  Méthodes en lecture seule — pas de modification en base
    // ══════════════════════════════════════════════════════════════

    /**
     * Recherche une mission par son ID.
     * Retourne un Optional vide si introuvable.
     */
    @Transactional(readOnly = true)
    public Optional<Mission> findById(Long id) {
        return missionRepository.findById(id);
    }

    /**
     * Retourne la mission de l'avocat associée à un dossier.
     * Utilisé dans le détail dossier pour afficher la mission judiciaire.
     * Retourne null si aucune mission avocat n'existe.
     */
    @Transactional(readOnly = true)
    public Mission getMissionAvocatDuDossier(Long dossierId) {
        List<Mission> missions = missionRepository.findMissionsAvocatDuDossier(dossierId);
        return missions.isEmpty() ? null : missions.get(0);
    }

    /**
     * Retourne toutes les missions assignées à un prestataire,
     * triées par date d'assignation décroissante (plus récente en premier).
     */
    @Transactional(readOnly = true)
    public List<Mission> getMissionsPrestataire(String username) {
        return missionRepository.findByPrestataire_UsernameOrderByDateAssignationDesc(username);
    }

    /**
     * Charge une mission avec toutes ses relations (prestataire, prestation, dossier).
     * Lève une exception si la mission est introuvable.
     */
    @Transactional(readOnly = true)
    public Mission getMissionWithDetails(Long id) {
        return missionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    }

    /**
     * Charge une mission et vérifie que l'utilisateur connecté
     * est bien le prestataire assigné à cette mission.
     * Lève AccessDeniedException si ce n'est pas le cas.
     */
    @Transactional(readOnly = true)
    public Mission getMissionForPrestataire(Long id, String username) {
        Mission mission = missionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Mission introuvable"));

        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new AccessDeniedException("Accès refusé");
        }
        return mission;
    }

    // ══════════════════════════════════════════════════════════════
    //  GESTION MISSION (AGENT)
    //  Création, modification, suppression — réservé à l'agent
    // ══════════════════════════════════════════════════════════════

    /**
     * Modifie la description et/ou la date de fin d'une mission.
     * CONTRAINTE : uniquement si la mission est au statut ASSIGNEE.
     * Une mission déjà en cours ne peut plus être modifiée.
     */
    @Transactional
    public Mission modifierMission(Long missionId, String description,
                                   LocalDate dateFinPrevue,
                                   String agentUsername) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (mission.getStatut() != StatutMission.ASSIGNEE) {
            throw new IllegalStateException(
                    "Impossible de modifier une mission au statut : " + mission.getStatut());
        }

        if (description != null && !description.isBlank()) {
            mission.setDescription(description);
        }
        if (dateFinPrevue != null) {
            mission.setDateFinPrevue(dateFinPrevue);
        }

        log.info("Mission {} modifiée par {}", mission.getNumeroMission(), agentUsername);
        return missionRepository.save(mission);
    }

    /**
     * Supprime définitivement une mission.
     * CONTRAINTE : uniquement si la mission est au statut ASSIGNEE.
     * Impossible de supprimer une mission déjà en cours de traitement.
     */
    @Transactional
    public void supprimerMission(Long missionId, String agentUsername) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (mission.getStatut() != StatutMission.ASSIGNEE) {
            throw new IllegalStateException(
                    "Impossible de supprimer une mission au statut : " + mission.getStatut());
        }

        log.info("Mission {} supprimée par {}", mission.getNumeroMission(), agentUsername);
        missionRepository.delete(mission);
    }

    /**
     * Change le statut d'une mission sans autre logique métier.
     * Utilisé par le prestataire pour passer EN_COURS, PV_SOUMIS, etc.
     * CONTRAINTE : interdit si la mission est ANNULEE.
     */
    @Transactional
    public void changerStatut(Long missionId, StatutMission nouveauStatut) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission annulée : modification de statut interdite");
        }

        mission.setStatut(nouveauStatut);
        log.info("Statut mission {} changé vers {}", missionId, nouveauStatut);
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 1 — PRESTATAIRE : SOUMETTRE LE PV
    //  Le prestataire soumet son procès-verbal après intervention
    // ══════════════════════════════════════════════════════════════

    /**
     * Permet au prestataire de soumettre le PV de sa mission.
     * Le statut passe à PV_SOUMIS.
     * CONTRAINTE : mission ne doit pas être ANNULEE.
     * Note : la vérification d'accès est faite dans le controller.
     */
    @Transactional
    public void soumettrePV(Long missionId, String pvTexte, String prestataireUsername) {
        Mission mission = missionRepository.findByIdWithDetails(missionId)
                .orElseThrow(() -> new EntityNotFoundException("Mission introuvable"));

        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new IllegalStateException(
                "Mission annulée — contactez l'agent pour la rouvrir");
        }

        mission.setStatut(StatutMission.PV_SOUMIS);
        mission.setPvMission(pvTexte);
        mission.setPvValide(true);

        log.info("PV soumis pour mission {} par {}",
                 mission.getNumeroMission(), prestataireUsername);
        missionRepository.save(mission);
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 2 — PRESTATAIRE : SOUMETTRE LA FACTURE
    //  Après le PV accepté, le prestataire envoie sa facture
    // ══════════════════════════════════════════════════════════════

    /**
     * Permet au prestataire de soumettre sa facture.
     * Le statut passe à FACTURE_SOUMISE.
     * CONTRAINTE : le PV doit avoir été soumis avant (statut PV_SOUMIS).
     * La facture sera ensuite examinée par le validateur financier.
     */
    @Transactional
    public void soumettreFacture(Long missionId, String factureRef,
                                  Double montantFacture, String username) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new IllegalStateException("Mission annulée — facture impossible.");
        }
        if (mission.getStatut() != StatutMission.PV_SOUMIS
                && mission.getStatut() != StatutMission.TERMINEE) {
            throw new IllegalStateException(
                "Soumettez d'abord le PV. Statut actuel : " + mission.getStatut());
        }

        mission.setFactureRef(factureRef);
        mission.setMontantFacture(montantFacture);
        mission.setFactureValide(null); // en attente de validation financière
        mission.setStatut(StatutMission.FACTURE_SOUMISE);

        log.info("Facture soumise pour mission {} par {}",
                 mission.getNumeroMission(), username);
        missionRepository.save(mission);
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 3 — VALIDATEUR FINANCIER : EXAMINER LA FACTURE
    //  Le validateur financier valide ou rejette la facture soumise
    // ══════════════════════════════════════════════════════════════

    /**
     * Le validateur financier VALIDE la facture du prestataire.
     * Le statut passe à FACTURE_VALIDEE.
     * Après cette étape, l'agent bancaire peut valider la mission.
     * CONTRAINTE : la facture doit être au statut FACTURE_SOUMISE.
     */
    @Transactional
    public void validerFactureParFinancier(Long id, String commentaire,
                                            String validateurUsername) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        if (mission.getStatut() != StatutMission.FACTURE_SOUMISE) {
            throw new RuntimeException(
                "Aucune facture à valider. Statut actuel : " + mission.getStatut());
        }
    
        mission.setFactureValide(true);
        mission.setStatut(StatutMission.FACTURE_VALIDEE);
        mission.setDateValidationFacture(LocalDateTime.now());
        mission.setValideParAgent(validateurUsername);
    
        if (commentaire != null && !commentaire.isBlank()) {
            mission.setCommentaireAgent(commentaire);
        }
    
        missionRepository.save(mission);
    
        try {
            String agentUsername       = mission.getPrestation().getDossier().getCreePar();
            String prestataireUsername = mission.getPrestataire().getUsername();
            String numeroMission       = mission.getNumeroMission();
            Long   dossierId           = mission.getPrestation().getDossier().getId();
    
            notificationService.notifier(
                agentUsername,
                "✅ Facture validée par le validateur financier",
                "La facture de la mission " + numeroMission
                    + " (dossier " + mission.getPrestation().getDossier().getNumeroDossier()
                    + ") a été validée par le validateur financier."
                    + " Vous pouvez maintenant clôturer la mission.",
                "VALIDATION_FINANCIERE_OK",
                mission.getPrestation().getDossier(),
                "/agent/dossiers/" + dossierId + "/resultats-prestataires?missionId=" + id
            );
    
            notificationService.notifierSansDossier(
                prestataireUsername,
                "✅ Votre facture a été validée",
                "Votre facture pour la mission " + numeroMission
                    + " a été validée par le validateur financier.",
                "VALIDATION_FINANCIERE_OK",
                "/prestataire/missions/" + id
            );
        } catch (Exception ex) {
            log.warn("Notification validation facture non envoyée — mission={} : {}",
                    id, ex.getMessage());
        }
    
        log.info("Facture mission {} validée par le financier {}",
                 mission.getNumeroMission(), validateurUsername);
    }
    /**
     * Le validateur financier REJETTE la facture du prestataire.
     * Le statut passe à FACTURE_REJETEE.
     * Le prestataire devra soumettre une nouvelle facture corrigée.
     * CONTRAINTE : la facture doit être au statut FACTURE_SOUMISE.
     */
    @Transactional
    public void rejeterFactureParFinancier(Long id, String commentaire,
                                            String validateurUsername) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        if (mission.getStatut() != StatutMission.FACTURE_SOUMISE) {
            throw new RuntimeException(
                "Aucune facture à rejeter. Statut actuel : " + mission.getStatut());
        }
    
        mission.setFactureValide(false);
        mission.setStatut(StatutMission.FACTURE_REJETEE);
        mission.setDateValidationFacture(LocalDateTime.now());
        mission.setValideParAgent(validateurUsername);
        mission.setCommentaireAgent(commentaire);
    
        missionRepository.save(mission);
    
        try {
            String agentUsername       = mission.getPrestation().getDossier().getCreePar();
            String prestataireUsername = mission.getPrestataire().getUsername();
            String numeroMission       = mission.getNumeroMission();
            Long   dossierId           = mission.getPrestation().getDossier().getId();
    
            notificationService.notifier(
                agentUsername,
                "❌ Facture rejetée — mission " + numeroMission,
                "La facture de la mission " + numeroMission
                    + " (dossier " + mission.getPrestation().getDossier().getNumeroDossier()
                    + ") a été rejetée par le validateur financier."
                    + (commentaire != null && !commentaire.isBlank() ? " Motif : " + commentaire : ""),
                "REJET_FINANCIER",
                mission.getPrestation().getDossier(),
                "/agent/dossiers/" + dossierId + "/resultats-prestataires?missionId=" + id
            );
    
            notificationService.notifierSansDossier(
                prestataireUsername,
                "❌ Votre facture a été rejetée",
                "Votre facture pour la mission " + numeroMission
                    + " a été rejetée."
                    + (commentaire != null && !commentaire.isBlank() ? " Motif : " + commentaire : "")
                    + " Merci de corriger et resoumettre.",
                "REJET_FINANCIER",
                "/prestataire/missions/" + id
            );
        } catch (Exception ex) {
            log.warn("Notification rejet facture non envoyée — mission={} : {}",
                    id, ex.getMessage());
        }
    
        log.info("Facture mission {} rejetée par le financier {}",
                 mission.getNumeroMission(), validateurUsername);
    }
   
    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 4 — AGENT BANCAIRE : VALIDER OU REJETER LA MISSION
    //  L'agent intervient UNIQUEMENT après validation du financier
    // ══════════════════════════════════════════════════════════════

    /**
     * L'agent bancaire VALIDE définitivement la mission.
     * Le statut passe à TERMINEE — fin du cycle de vie.
     * CONTRAINTE IMPORTANTE : la facture doit avoir été validée
     * par le validateur financier (statut FACTURE_VALIDEE).
     * L'agent ne peut pas valider directement sans ce passage.
     */
    @Transactional
    public void validerMission(Long id, String commentaire, String agentUsername) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission annulée : validation impossible");
        }

        if (mission.getStatut() != StatutMission.FACTURE_VALIDEE
                && mission.getStatut() != StatutMission.FACTURE_PAYEE) {
            throw new RuntimeException(
                "La facture doit être validée par le validateur financier avant. " +
                "Statut actuel : " + mission.getStatut());
        }

        mission.setStatut(StatutMission.TERMINEE);
        mission.setCommentaireAgent(commentaire);
        mission.setValideParAgent(agentUsername);
        mission.setDateValidationAgent(LocalDateTime.now());

        log.info("Mission {} validée (TERMINEE) par l'agent {}",
                 mission.getNumeroMission(), agentUsername);
        missionRepository.save(mission);
    }

    /**
     * L'agent bancaire REJETTE la mission après examen.
     * Le statut passe à REJETEE.
     * Le prestataire devra resoumettre son PV ET sa facture.
     * CONTRAINTE : impossible de rejeter une mission déjà TERMINEE ou déjà REJETEE.
     */
    @Transactional
    public void rejeterMission(Long id, String commentaire, String agentUsername) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));

        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new RuntimeException("Mission déjà validée : rejet impossible");
        }
        if (mission.getStatut() == StatutMission.REJETEE) {
            throw new RuntimeException("Mission déjà rejetée");
        }

        mission.setStatut(StatutMission.REJETEE);
        mission.setCommentaireAgent(commentaire);
        mission.setValideParAgent(agentUsername);
        mission.setDateValidationAgent(LocalDateTime.now());

        log.info("Mission {} rejetée par l'agent {}",
                 mission.getNumeroMission(), agentUsername);
        missionRepository.save(mission);
    }

    // ══════════════════════════════════════════════════════════════
    //  RESOUMISSION PRESTATAIRE APRÈS REJET
    //  Deux cas : rejet agent (PV + facture) ou rejet financier (facture seule)
    // ══════════════════════════════════════════════════════════════

    /**
     * CAS 1 — Rejet par l'AGENT BANCAIRE.
     * Le prestataire resoumet son PV après rejet complet de la mission.
     * Reset total : PV, facture, commentaires, dates de validation.
     * Le statut repasse à PV_SOUMIS pour recommencer le cycle.
     * CONTRAINTE : mission doit être au statut REJETEE.
     */
    @Transactional
    public void resoumettreApresRejetAgent(Long missionId, String pvTexte,
                                            String username) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (mission.getStatut() != StatutMission.REJETEE) {
            throw new IllegalStateException(
                "Impossible de resoumettre — statut actuel : " + mission.getStatut());
        }

        if (mission.getPrestataire() == null ||
            !username.equals(mission.getPrestataire().getUsername())) {
            throw new RuntimeException("Accès refusé");
        }

        // Reset complet avant resoumission
        mission.setPvMission(pvTexte);
        mission.setPvValide(false);
        mission.setFactureRef(null);
        mission.setMontantFacture(null);
        mission.setFactureValide(null);
        mission.setCommentaireAgent(null);
        mission.setValideParAgent(null);
        mission.setDateValidationAgent(null);
        mission.setDateValidationFacture(null);
        mission.setStatut(StatutMission.PV_SOUMIS);

        log.info("Mission {} resoumise (PV) après rejet agent par {}",
                mission.getNumeroMission(), username);
        missionRepository.save(mission);
    }

    /**
     * CAS 2 — Rejet par le VALIDATEUR FINANCIER.
     * Le prestataire resoumet uniquement sa facture corrigée.
     * Le PV existant est conservé — pas besoin de le resoumettre.
     * Le statut repasse à FACTURE_SOUMISE pour réexamen financier.
     * CONTRAINTE : mission doit être au statut FACTURE_REJETEE.
     */
    @Transactional
    public void resoumettreFactureApresRejetFinancier(Long missionId,
                                                       String factureRef,
                                                       Double montantFacture,
                                                       String username) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (mission.getStatut() != StatutMission.FACTURE_REJETEE) {
            throw new IllegalStateException(
                "Impossible de resoumettre — statut actuel : " + mission.getStatut());
        }

        if (mission.getPrestataire() == null ||
            !username.equals(mission.getPrestataire().getUsername())) {
            throw new RuntimeException("Accès refusé");
        }

        // Reset uniquement la partie facture — le PV reste intact
        mission.setFactureRef(factureRef);
        mission.setMontantFacture(montantFacture);
        mission.setFactureValide(null);
        mission.setCommentaireAgent(null);
        mission.setValideParAgent(null);
        mission.setDateValidationFacture(null);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);

        log.info("Facture resoumise après rejet financier — mission {} par {}",
                mission.getNumeroMission(), username);
        missionRepository.save(mission);
    }

    // ══════════════════════════════════════════════════════════════
    //  ÉTAPE 3bis — VALIDATEUR FINANCIER : ÉMETTRE LE PAIEMENT
    //  Une fois la facture validée (FACTURE_VALIDEE), le validateur
    //  émet un virement ou un chèque BCT pour régler le prestataire.
    // ══════════════════════════════════════════════════════════════

    /**
     * Enregistre le paiement (virement ou chèque BCT) d'une facture
     * de mission déjà validée par le validateur financier.
     * CONTRAINTE : la facture doit être au statut FACTURE_VALIDEE.
     */
    @Transactional
    public void effectuerPaiementFacture(Long missionId,
                                          ModePaiement mode,
                                          String reference,
                                          String beneficiaireRib,
                                          CompteBancaireAgence compte,
                                          String validateurUsername) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        if (!Boolean.TRUE.equals(mission.getFactureValide())) {
            throw new IllegalStateException(
                "La facture doit être validée avant d'émettre un paiement.");
        }

        if (mode == ModePaiement.VIREMENT && (beneficiaireRib == null || beneficiaireRib.isBlank())) {
            throw new IllegalArgumentException("Le RIB du bénéficiaire est requis pour un virement.");
        }

        mission.setPaiementMode(mode);
        mission.setPaiementReference(reference);
        mission.setPaiementDate(LocalDate.now());
        mission.setPaiementBeneficiaireNom(
            mission.getPrestataire() != null
                ? (mission.getPrestataire().getPrenom() + " " + mission.getPrestataire().getNom()).trim()
                : null
        );
        mission.setPaiementBeneficiaireRib(mode == ModePaiement.VIREMENT ? beneficiaireRib : null);
        mission.setPaiementCompteAgenceBanque(compte.getBanque());
        mission.setPaiementCompteAgenceRib(compte.getRib());
        mission.setPaiementEffectuePar(validateurUsername);

        // Le statut ne progresse que si l'agent n'a pas déjà clôturé la mission (TERMINEE)
        if (mission.getStatut() == StatutMission.FACTURE_VALIDEE) {
            mission.setStatut(StatutMission.FACTURE_PAYEE);
        }

        missionRepository.save(mission);

        log.info("Paiement ({}) émis pour mission {} par {}",
                 mode, mission.getNumeroMission(), validateurUsername);
    }

    // ══════════════════════════════════════════════════════════════
    //  STATISTIQUES
    //  Utilisées pour les dashboards (avocat, agent, prestataire)
    // ══════════════════════════════════════════════════════════════

    /**
     * Compte le nombre de missions d'un prestataire/avocat
     * pour un statut donné. Utilisé dans le dashboard avocat.
     */
    @Transactional(readOnly = true)
    public long countByStatutAndAvocat(String username, StatutMission statut) {
        return missionRepository
                .findByPrestataire_UsernameAndStatut(username, statut)
                .size();
    }

    /**
     * Calcule le total des honoraires facturés par un prestataire/avocat.
     * Additionne tous les montants de facture non nuls.
     * Utilisé dans le dashboard avocat pour afficher le CA total.
     */
    @Transactional(readOnly = true)
    public double totalHonoraires(String username) {
        return missionRepository
                .findByPrestataire_Username(username)
                .stream()
                .filter(m -> m.getMontantFacture() != null)
                .mapToDouble(Mission::getMontantFacture)
                .sum();
    }
}