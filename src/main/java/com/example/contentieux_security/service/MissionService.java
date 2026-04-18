package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    public Mission getMissionWithDetails(Long id) {
        return missionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable : " + id));
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
    
        // ❌ sécurité métier
        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission rejetée : validation impossible");
        }
    
        mission.setStatut(StatutMission.TERMINEE);
        mission.setCommentaireAgent(commentaire);
        mission.setValideParAgent(agentUsername);
        mission.setDateValidationAgent(LocalDateTime.now());
    
        missionRepository.save(mission);
    }
   
    // ── REJET MISSION ───────────────────────
    @Transactional
    public void rejeterMission(Long id, String commentaire, String agentUsername) {
    
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        // ❌ éviter double traitement
        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new RuntimeException("Mission déjà validée : rejet impossible");
        }
    
        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new RuntimeException("Mission déjà rejetée");
        }
    
        mission.setStatut(StatutMission.ANNULEE);
        mission.setCommentaireAgent(commentaire);
        mission.setValideParAgent(agentUsername);
        mission.setDateValidationAgent(LocalDateTime.now());
    
        missionRepository.save(mission);
    }



    public Mission getMissionForPrestataire(Long id, String username) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    
        if (mission.getPrestataire() == null ||
            !mission.getPrestataire().getUsername().equals(username)) {
            throw new RuntimeException("Accès refusé");
        }
    
        return mission;
    }

    @Transactional
    public void soumettrePV(Long missionId, String pvTexte, String prestataireUsername) {
        Mission mission = getMissionForPrestataire(missionId, prestataireUsername);
        
        // ❌ BLOCAGE : Si ANNULEE, impossible
        if (mission.getStatut() == StatutMission.ANNULEE) {
            throw new IllegalStateException(
                "Mission annulée - Contactez l'agent pour la rouvrir");
        }
        
        if (mission.getStatut() != StatutMission.EN_COURS) {
            throw new IllegalStateException(
                "Impossible de soumettre PV - Statut: " + mission.getStatut());
        }
        
        mission.setStatut(StatutMission.PV_SOUMIS);
        mission.setPvMission(pvTexte);        mission.setPvValide(true);
        log.info("PV soumis pour mission {} par {}", mission.getNumeroMission(), prestataireUsername);
    }

}