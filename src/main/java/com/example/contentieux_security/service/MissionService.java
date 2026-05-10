package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;

// Entités — adapte le package à ton projet
import com.example.contentieux_security.entity.Mission;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;

    // ── FIND BY ID ─────────────────────────────
    @Transactional(readOnly = true)
    public Optional<Mission> findById(Long id) {
        return missionRepository.findById(id);
    }

    // ── MISSION AVOCAT DU DOSSIER ─────────────
    @Transactional(readOnly = true)
    public Mission getMissionAvocatDuDossier(Long dossierId) {
        List<Mission> missions = missionRepository.findMissionsAvocatDuDossier(dossierId);
        return missions.isEmpty() ? null : missions.get(0);
    }

    // ── LISTE PRESTATAIRE ─────────────────────
    @Transactional(readOnly = true)
    public List<Mission> getMissionsPrestataire(String username) {
        return missionRepository.findByPrestataire_UsernameOrderByDateAssignationDesc(username);
    }

    @Transactional(readOnly = true)
   // MissionService.java
public Mission getMissionWithDetails(Long id) {
    return missionRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Mission introuvable"));
}

    // ── MODIFIER MISSION ─────────────────────
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

    // ── SUPPRIMER MISSION ─────────────────────
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

    // ── CHANGER STATUT ───────────────────────
    @Transactional
    public void changerStatut(Long missionId, StatutMission nouveauStatut) {
    
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        // ❌ BLOQUAGE IMPORTANT
        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission rejetée : modification interdite");
        }
    
        mission.setStatut(nouveauStatut);
    }

    // ── VALIDATION AGENT (PV + FACTURE) ──────
    @Transactional
    public void validerMission(Long id, String commentaire, String agentUsername) {
    
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission rejetée : validation impossible");
        }
    
        // ✅ L'agent ne peut valider que si la facture a été validée par le financier
        if (mission.getStatut() != StatutMission.FACTURE_VALIDEE) {
            throw new RuntimeException(
                "La facture doit être validée par le validateur financier avant votre validation. Statut actuel : "
                + mission.getStatut());
        }
    
        mission.setStatut(StatutMission.TERMINEE);
        mission.setCommentaireAgent(commentaire);
        mission.setValideParAgent(agentUsername);
        mission.setDateValidationAgent(LocalDateTime.now());
    
        log.info("Mission {} validée par l'agent {}", mission.getNumeroMission(), agentUsername);
        missionRepository.save(mission);
    }

    // ── VALIDATION PAR LE VALIDATEUR FINANCIER ──
@Transactional
public void validerFactureParFinancier(Long id, String commentaire, String validateurUsername) {

    Mission mission = missionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mission introuvable"));

    if (mission.getStatut() != StatutMission.FACTURE_SOUMISE) {
        throw new RuntimeException(
            "Aucune facture à valider. Statut actuel : " + mission.getStatut());
    }

    mission.setFactureValide(true);
    mission.setStatut(StatutMission.FACTURE_VALIDEE);
    mission.setDateValidationFacture(LocalDateTime.now());
    mission.setValideParAgent(validateurUsername); // ✅ stocke le nom du validateur financier

    if (commentaire != null && !commentaire.isBlank()) {
        mission.setCommentaireAgent(commentaire);
    }

    log.info("Facture mission {} validée par le financier {}",
             mission.getNumeroMission(), validateurUsername);
    missionRepository.save(mission);
}

// ── REJET FACTURE PAR LE VALIDATEUR FINANCIER ──
@Transactional
public void rejeterFactureParFinancier(Long id, String commentaire, String validateurUsername) {

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

    log.info("Facture mission {} rejetée par le financier {}",
             mission.getNumeroMission(), validateurUsername);
    missionRepository.save(mission);
}
    // ── REJET MISSION ───────────────────────
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
    
        log.info("Mission {} rejetée par l'agent {}", mission.getNumeroMission(), agentUsername);
        missionRepository.save(mission);
    }
// MissionService.java
@Transactional(readOnly = true)
public Mission getMissionForPrestataire(Long id, String username) {
    Mission mission = missionRepository.findByIdWithDetails(id)  // ← était findByIdWithPrestataire
            .orElseThrow(() -> new EntityNotFoundException("Mission introuvable"));

    if (!mission.getPrestataire().getUsername().equals(username)) {
        throw new AccessDeniedException("Accès refusé");
    }
    return mission;
}
    
   // MissionService.java — corriger soumettrePV
   @Transactional
   public void soumettrePV(Long missionId, String pvTexte, String prestataireUsername) {
       Mission mission = missionRepository.findByIdWithDetails(missionId)
               .orElseThrow(() -> new EntityNotFoundException("Mission introuvable"));
   
       // ✅ Vérification accès — accepte soit le prestataire soit l'avocat de l'affaire
       boolean isOwner = false;
   
       if (mission.getPrestataire() != null &&
           mission.getPrestataire().getUsername().equals(prestataireUsername)) {
           isOwner = true;
       }
   
       // Cherche aussi via l'affaire judiciaire liée
       if (!isOwner) {
           boolean avocatMatch = missionRepository
                   .findByIdWithDetails(missionId)
                   .map(m -> m.getPrestataire())
                   .map(p -> p.getUsername().equals(prestataireUsername))
                   .orElse(false);
           isOwner = avocatMatch;
       }
   
       // ⚠️ Ne pas bloquer — la vérification est faite dans le controller
       // isOwner check retiré ici car AvocatAffaireController vérifie via isAvocatOwner
   
       if (mission.getStatut() == StatutMission.ANNULEE) {
           throw new IllegalStateException(
               "Mission annulée - Contactez l'agent pour la rouvrir");
       }
   
       if (mission.getStatut() != StatutMission.ANNULEE) {
           throw new IllegalStateException(
               "Impossible de soumettre PV - Statut: " + mission.getStatut());
       }
   
       mission.setStatut(StatutMission.PV_SOUMIS);
       mission.setPvMission(pvTexte);
       mission.setPvValide(true);
       log.info("PV soumis pour mission {} par {}",
                mission.getNumeroMission(), prestataireUsername);
   }
   
   // ─────────────────────────────────────────────
//  FACTURE — soumettre
// ─────────────────────────────────────────────
@Transactional
public void soumettreFacture(Long missionId,
                              String factureRef,
                              Double montantFacture,
                              String username) {
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
    mission.setFactureValide(true);
    mission.setStatut(StatutMission.FACTURE_SOUMISE);
    log.info("Facture soumise pour mission {} par {}", mission.getNumeroMission(), username);
    missionRepository.save(mission);
}



// ─────────────────────────────────────────────
//  STATS — pour dashboard avocat
// ─────────────────────────────────────────────
@Transactional(readOnly = true)
public long countByStatutAndAvocat(String username, StatutMission statut) {
    return missionRepository
            .findByPrestataire_UsernameAndStatut(username, statut)
            .size();
}

@Transactional(readOnly = true)
public double totalHonoraires(String username) {
    return missionRepository
            .findByPrestataire_Username(username)
            .stream()
            .filter(m -> m.getMontantFacture() != null)
            .mapToDouble(Mission::getMontantFacture)
            .sum();
}




// prestataire de resoumettre PV + facture après rejet.

@Transactional
public void resoumettreApresRejet(Long missionId,
                                   String pvTexte,
                                   String username) {
    Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

    if (mission.getStatut() != StatutMission.REJETEE) {
        throw new IllegalStateException(
                "Impossible de resoumettre — statut actuel : " + mission.getStatut());
    }

    // Vérifier que c'est bien le prestataire de la mission
    if (mission.getPrestataire() == null ||
        !username.equals(mission.getPrestataire().getUsername())) {
        throw new RuntimeException("Accès refusé");
    }

    // Reset complet : repartir de EN_COURS avec nouveau PV
    mission.setPvMission(pvTexte);
    mission.setPvValide(false);
    mission.setFactureRef(null);
    mission.setMontantFacture(null);
    mission.setFactureValide(false);
    mission.setCommentaireAgent(null);
    mission.setStatut(StatutMission.PV_SOUMIS);

    log.info("Mission {} resoumise après rejet par {}",
            mission.getNumeroMission(), username);
    missionRepository.save(mission);
}

@Transactional
public void resoumettreFactureApresRejet(Long missionId,
                                          String factureRef,
                                          Double montantFacture,
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

    // Garder le PV existant, resoumettre uniquement la facture
    mission.setFactureRef(factureRef);
    mission.setMontantFacture(montantFacture);
    mission.setFactureValide(false);
    mission.setCommentaireAgent(null);
    mission.setStatut(StatutMission.FACTURE_SOUMISE);

    log.info("Facture resoumise après rejet mission {} par {}",
            mission.getNumeroMission(), username);
    missionRepository.save(mission);
}



}