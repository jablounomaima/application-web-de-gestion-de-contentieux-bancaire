package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.HistoriqueDossier;
import com.example.contentieux_security.repository.HistoriqueDossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoriqueService {

    private final HistoriqueDossierRepository historiqueRepository;

    // ── Constantes d'actions ──────────────────────────────────────
    public static final String CREATION            = "CREATION";
    public static final String MODIFICATION        = "MODIFICATION";
    public static final String SOUMISSION          = "SOUMISSION";
    public static final String VALIDATION_FIN      = "VALIDATION_FIN";
    public static final String REJET_FIN           = "REJET_FIN";
    public static final String VALIDATION_JUR      = "VALIDATION_JUR";
    public static final String REJET_JUR           = "REJET_JUR";
    public static final String AJOUT_RISQUE        = "AJOUT_RISQUE";
    public static final String SELECTION_RISQUE    = "SELECTION_RISQUE";
    public static final String AJOUT_GARANTIE      = "AJOUT_GARANTIE";
    public static final String LANCEMENT_PROCEDURE = "LANCEMENT_PROCEDURE";
    public static final String CLOTURE             = "CLOTURE";

    /**
     * Enregistre une action dans l'historique.
     *
     * PROPAGATION.REQUIRED (et non plus REQUIRES_NEW) :
     * - Participe à la transaction appelante si elle existe.
     * - Évite le lock wait timeout qui survenait quand REQUIRES_NEW
     *   ouvrait une 2e connexion et attendait le verrou de la 1ère
     *   transaction sur la ligne dossier (pas encore committée).
     * - L'historique est quand même sauvegardé : si la transaction
     *   principale échoue, tout est rollbacké ensemble (cohérence).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enregistrer(DossierContentieux dossier, String typeAction,
                             String description, String utilisateur) {
        historiqueRepository.save(
            HistoriqueDossier.builder()
                .dossier(dossier)
                .typeAction(typeAction)
                .description(description)
                .utilisateur(utilisateur)
                .dateAction(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Variante avec dossierId uniquement (évite de charger l'entité entière).
     * Utile quand on n'a pas l'objet DossierContentieux sous la main.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enregistrerParId(Long dossierId, String typeAction,
                                  String description, String utilisateur) {
        DossierContentieux ref = new DossierContentieux();
        ref.setId(dossierId);   // proxy sans chargement complet
        historiqueRepository.save(
            HistoriqueDossier.builder()
                .dossier(ref)
                .typeAction(typeAction)
                .description(description)
                .utilisateur(utilisateur)
                .dateAction(LocalDateTime.now())
                .build()
        );
    }

    /** Historique complet d'un dossier, du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<HistoriqueDossier> getHistorique(Long dossierId) {
        return historiqueRepository.findByDossier_IdOrderByDateActionDesc(dossierId);
    }


    @Transactional
public void supprimerParDossier(Long dossierId) {
    historiqueRepository.deleteByDossierId(dossierId);
}
}