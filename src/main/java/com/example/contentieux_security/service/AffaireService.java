package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AffaireService {

    private final MissionRepository        missionRepository;
    private final AffaireJudiciaireRepository affaireJudiciaireRepository;

    // =========================================================
    //  📄 Soumettre PV — réservé à l'avocat
    //  L'avocat travaille sur une affaire, pas une mission
    //  On accepte ASSIGNEE et EN_COURS
    // =========================================================
    @Transactional
    public void soumettreAvocatPV(Long affaireId, String pvTexte) {

        AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        Mission mission = affaire.getMission();
        if (mission == null) {
            throw new RuntimeException("Aucune mission liée à cette affaire.");
        }

        if (mission.getStatut() != StatutMission.ASSIGNEE
                && mission.getStatut() != StatutMission.EN_COURS) {
            throw new RuntimeException(
                "PV déjà soumis ou mission dans un état invalide : " + mission.getStatut());
        }

        mission.setPvMission(pvTexte);
        mission.setStatut(StatutMission.PV_SOUMIS);
        mission.setDateValidationPv(LocalDateTime.now());
        missionRepository.save(mission);

        log.info("PV soumis par avocat pour affaireId={} missionId={}", affaireId, mission.getId());
    }

    // =========================================================
    //  🧾 Soumettre Facture — réservé à l'avocat
    //  On accepte PV_SOUMIS uniquement
    // =========================================================
    @Transactional
    public void soumettreAvocatFacture(Long affaireId, String factureRef, Double montant) {

        AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        Mission mission = affaire.getMission();
        if (mission == null) {
            throw new RuntimeException("Aucune mission liée à cette affaire.");
        }

        if (mission.getStatut() != StatutMission.PV_SOUMIS) {
            throw new RuntimeException(
                "Facture non disponible — soumettez d'abord le PV. Statut actuel : "
                + mission.getStatut());
        }

        mission.setFactureRef(factureRef);
        mission.setMontantFacture(montant);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);
        mission.setDateValidationFacture(LocalDateTime.now());
        missionRepository.save(mission);

        log.info("Facture soumise par avocat pour affaireId={} missionId={}", affaireId, mission.getId());
    }

    // =========================================================
    //  🔍 Trouver l'affaire par missionId
    // =========================================================
    public AffaireJudiciaire findByMissionId(Long missionId) {
        return affaireJudiciaireRepository.findByMission_Id(missionId)
                .orElse(null);
    }
}