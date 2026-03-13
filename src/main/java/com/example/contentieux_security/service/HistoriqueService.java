package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Dossier;
import com.example.contentieux_security.entity.HistoriqueDossier;
import com.example.contentieux_security.repository.HistoriqueDossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de traçabilité — enregistre chaque action métier dans l'historique du dossier.
 * Utilisé dans DossierService et ValidationService.
 */
@Service
@RequiredArgsConstructor
public class HistoriqueService {

    private final HistoriqueDossierRepository historiqueRepository;

    // Types d'actions standards
    public static final String CREATION              = "CREATION";
    public static final String MODIFICATION          = "MODIFICATION";
    public static final String SOUMISSION            = "SOUMISSION";
    public static final String VALIDATION_FINANCIERE = "VALIDATION_FIN";
    public static final String REJET_FINANCIER       = "REJET_FIN";
    public static final String VALIDATION_JURIDIQUE  = "VALIDATION_JUR";
    public static final String REJET_JURIDIQUE       = "REJET_JUR";
    public static final String AJOUT_RISQUE          = "AJOUT_RISQUE";
    public static final String SELECTION_RISQUE      = "SELECTION_RISQUE";
    public static final String AJOUT_GARANTIE        = "AJOUT_GARANTIE";
    public static final String LANCEMENT_PROCEDURE   = "LANCEMENT_PROCEDURE";
    public static final String CLOTURE               = "CLOTURE";

    /**
     * Enregistre une entrée dans l'historique.
     * Propagation REQUIRES_NEW = l'historique est sauvegardé même si la transaction principale échoue.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(Dossier dossier, String typeAction,
                             String description, String utilisateur) {
        HistoriqueDossier h = HistoriqueDossier.builder()
                .dossier(dossier)
                .typeAction(typeAction)
                .description(description)
                .utilisateur(utilisateur)
                .dateAction(LocalDateTime.now())
                .build();
        historiqueRepository.save(h);
    }

    /**
     * Récupère l'historique complet d'un dossier, du plus récent au plus ancien.
     */
    public List<HistoriqueDossier> getHistorique(Long dossierId) {
        return historiqueRepository.findByDossier_IdOrderByDateActionDesc(dossierId);
    }
}