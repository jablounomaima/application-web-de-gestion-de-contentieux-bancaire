package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MissionService {

    private final MissionRepository missionRepo;

    /**
     * Récupère la mission avocat active d'un dossier
     */
    @Transactional(readOnly = true)
    public Mission getMissionAvocatDuDossier(Long dossierId) {
        return missionRepo.findMissionAvocatDuDossier(dossierId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Mission> getMissionsPrestataire(String username) {
        return missionRepo.findByPrestataire_UsernameOrderByDateAssignationDesc(username);
    }

    @Transactional(readOnly = true)
    public Mission getMissionWithDetails(Long id) {
        return missionRepo.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + id));
    }

    // ── Modification ──────────────────────────────────────────────────────
    @Transactional
    public Mission modifierMission(Long missionId, String description,
                                    java.time.LocalDate dateFinPrevue,
                                    String agentUsername) {

        Mission mission = missionRepo.findById(missionId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        // Seules les missions ASSIGNEE sont modifiables
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
        return missionRepo.save(mission);
    }

    // ── Suppression ───────────────────────────────────────────────────────
    @Transactional
    public void supprimerMission(Long missionId, String agentUsername) {

        Mission mission = missionRepo.findById(missionId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        // Seules les missions ASSIGNEE peuvent être supprimées
        if (mission.getStatut() != StatutMission.ASSIGNEE) {
            throw new IllegalStateException(
                "Impossible de supprimer une mission au statut : " + mission.getStatut());
        }

        log.info("Mission {} supprimée par {}", mission.getNumeroMission(), agentUsername);
        missionRepo.delete(mission);
    }


    @Transactional
public void changerStatut(Long missionId, StatutMission nouveauStatut) {
    Mission mission = missionRepo.findById(missionId)
        .orElseThrow(() -> new RuntimeException("Mission introuvable"));
    mission.setStatut(nouveauStatut);
    missionRepo.save(mission);
}
}