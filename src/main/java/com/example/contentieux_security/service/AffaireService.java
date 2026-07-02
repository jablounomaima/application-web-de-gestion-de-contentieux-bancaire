package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ⚠️ Cette classe fait doublon avec AffaireJudiciaireService (mêmes opérations
 * PV/Facture, sur la même entité). Elle est corrigée ici pour compiler et
 * rester cohérente avec la règle métier (AffaireJudiciaire est autonome,
 * sans lien vers Mission), mais elle mérite d'être fusionnée ou supprimée
 * une fois que vous aurez identifié qui l'appelle réellement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AffaireService {

    private final AffaireJudiciaireRepository affaireJudiciaireRepository;

    // =========================================================
    //  📄 Soumettre PV — réservé à l'avocat
    //  ❌ Ne passe plus par Mission : écrit directement sur l'affaire
    // =========================================================
    @Transactional
    public void soumettreAvocatPV(Long affaireId, String pvTexte) {

        AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        // Empêche de re-soumettre un PV déjà validé
        if (affaire.getPvStatut() == AffaireJudiciaire.StatutPV.VALIDE) {
            throw new RuntimeException("PV déjà validé pour cette affaire.");
        }

        affaire.setPvTexte(pvTexte);
        affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);
        affaireJudiciaireRepository.save(affaire);

        log.info("PV soumis par avocat pour affaireId={}", affaireId);
    }

    // =========================================================
    //  🧾 Soumettre Facture — réservé à l'avocat
    //  ❌ Ne passe plus par Mission : écrit directement sur l'affaire
    //  ✔ On garde la règle métier : le PV doit être soumis avant la facture
    // =========================================================
    @Transactional
    public void soumettreAvocatFacture(Long affaireId, String factureRef, Double montant) {

        AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        if (affaire.getPvStatut() == null) {
            throw new RuntimeException(
                "Facture non disponible — soumettez d'abord le PV.");
        }

        affaire.setFactureRef(factureRef);
        affaire.setMontantFacture(montant);
        affaire.setFactureStatut(AffaireJudiciaire.StatutFacture.EN_ATTENTE);
        affaireJudiciaireRepository.save(affaire);

        log.info("Facture soumise par avocat pour affaireId={}", affaireId);
    }

    // =========================================================
    //  🔍 Trouver l'affaire par missionId
    // =========================================================
    // ⚠️ DÉPRÉCIÉ : AffaireJudiciaire n'a plus de mission_id, donc cette
    // recherche n'a plus de sens. Conservée uniquement pour ne pas casser
    // la compilation chez ses appelants — retourne toujours null.
    // Identifiez ses appelants (grep "findByMissionId") et remplacez-les
    // par findById(affaireId) ou getAffaireParDossier(dossierId).
    @Deprecated
    public AffaireJudiciaire findByMissionId(Long missionId) {
        log.warn("findByMissionId({}) appelée mais obsolète : AffaireJudiciaire "
                + "n'a plus de relation Mission. Retourne null.", missionId);
        return null;
    }
}