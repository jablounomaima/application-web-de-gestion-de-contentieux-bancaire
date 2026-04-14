package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AffaireJudiciaireService {

    private final AffaireJudiciaireRepository affaireRepo;
    private final AudienceRepository          audienceRepo;
    private final DocumentAffaireRepository   documentRepo;
    // Add to the field declarations at the top of AffaireJudiciaireService
private final MissionRepository missionRepository;
    // =========================================================
    //  AFFAIRE
    // =========================================================

   // Replace the old 2-param creerAffaire with this 5-param version
public AffaireJudiciaire creerAffaire(Long missionId,
    String tribunal,
    String numeroRole,
    String chambre,
    String username) {
Mission mission = missionRepository.findById(missionId)
.orElseThrow(() -> new IllegalArgumentException(
"Mission introuvable : id=" + missionId));

// Prevent duplicates
affaireRepo.findByMission_Id(missionId).ifPresent(a -> {
throw new IllegalStateException(
"Une affaire existe déjà pour cette mission : " + a.getNumeroAffaire());
});

AffaireJudiciaire affaire = new AffaireJudiciaire();
affaire.setNumeroAffaire(genererNumeroAffaire());
affaire.setTribunal(tribunal);
affaire.setNumeroRole(numeroRole);
affaire.setChambre(chambre);
affaire.setDateLancement(LocalDate.now());
affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
affaire.setMission(mission);
affaire.setDossier(mission.getPrestation().getDossier());

return affaireRepo.save(affaire);
}

// Add this new method
public AffaireJudiciaire getAffaireParDossier(Long dossierId) {
return affaireRepo.findByDossier_Id(dossierId).orElse(null);
}

// Private helper — add at the bottom of the service
private String genererNumeroAffaire() {
long count = affaireRepo.count() + 1;
return String.format("AFF-%d-%05d", LocalDate.now().getYear(), count);
}
    public AffaireJudiciaire getAffaireById(Long id) {
        return affaireRepo.findById(id).orElse(null);
    }

    public List<AffaireJudiciaire> getAffairesParAvocat(String username) {
        return affaireRepo.findByMission_Prestataire_Username(username);
    }

    // =========================================================
    //  AUDIENCES
    // =========================================================

    public Audience ajouterAudience(Long affaireId, LocalDate dateAudience,
                                    String heure, String salle, String motif) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);
        if (affaire == null) return null;

        Audience audience = new Audience();
        audience.setDateAudience(dateAudience);
        audience.setHeure(heure);
        audience.setSalle(salle);
        audience.setMotif(motif);
        audience.setAffaire(affaire);
        return audienceRepo.save(audience);
    }

    // Signature matches controller exactly: (Long, String, StatutAudience, LocalDate)
    public Audience enregistrerResultatAudience(Long audienceId,
                                                String resultat,
                                                StatutAudience statut,
                                                LocalDate prochaineAudience) {
        Audience audience = audienceRepo.findById(audienceId).orElse(null);
        if (audience == null) return null;

        audience.setResultat(resultat);
        audience.setStatut(statut);
audienceRepo.save(audience);
        if (prochaineAudience != null) {
            audience.setProchaineAudience(prochaineAudience);
            AffaireJudiciaire affaire = audience.getAffaire();
            if (affaire != null) {
                affaire.setDateProchainAudience(prochaineAudience);
                affaireRepo.save(affaire);
            }
        }

        return audienceRepo.save(audience);
    }

    // =========================================================
    //  JUGEMENT
    // =========================================================

    // Signature matches controller exactly: 6 params
    public AffaireJudiciaire enregistrerJugement(Long affaireId,
                                                  String typeJugement,
                                                  LocalDate dateJugement,
                                                  String montantJuge,
                                                  String delaiPaiement,
                                                  String description) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);
        if (affaire == null) return null;

        try {
            affaire.setTypeJugement(AffaireJudiciaire.TypeJugement.valueOf(typeJugement));
        } catch (IllegalArgumentException e) {
            log.warn("TypeJugement inconnu: {}, ignoré", typeJugement);
        }

        affaire.setDateJugement(dateJugement);
        affaire.setMontantJuge(montantJuge);
        affaire.setDelaiPaiementJuge(delaiPaiement);
        affaire.setDescriptionJugement(description);
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.JUGEMENT_RENDU);

        return affaireRepo.save(affaire);
    }

    // =========================================================
    //  TRIBUNAL
    // =========================================================

    public void modifierTribunal(Long affaireId, String tribunalNom,
                                  String tribunalVille, String tribunalChambre,
                                  String numeroRole, String observations) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);
        if (affaire != null) {
            affaire.setTribunal(tribunalNom);
            affaire.setChambre(tribunalChambre);
            affaire.setNumeroRole(numeroRole);
            // tribunalVille: not a field on the entity, ignored
            affaireRepo.save(affaire);
        }
    }

    // =========================================================
    //  DOCUMENTS
    // =========================================================

    // Parameter type is DocumentAffaire.TypeDocument (enum), not String
    public DocumentAffaire uploadDocument(Long affaireId,
                                           MultipartFile file,
                                           DocumentAffaire.TypeDocument typeDocument,
                                           String description,
                                           String uploadeePar) throws IOException {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);

        DocumentAffaire doc = new DocumentAffaire();
        doc.setNomFichierOriginal(file.getOriginalFilename());
        doc.setNomFichierServeur(file.getOriginalFilename()); // override in real impl
        doc.setCheminFichier("");                             // set real path in impl
        doc.setTypeMime(file.getContentType());
        doc.setTailleFichier(file.getSize());
        doc.setTypeDocument(typeDocument);
        doc.setDescription(description);
        doc.setUploadeePar(uploadeePar);
        doc.setAffaire(affaire);
        return documentRepo.save(doc);
    }

    public DocumentAffaire getDocumentById(Long id) {
        return documentRepo.findById(id).orElse(null);
    }

    public void supprimerDocument(Long documentId, String username) {
        documentRepo.deleteById(documentId);
    }




    // =========================================================
//  AUDIENCES - Méthodes pour l'avocat
// =========================================================

/**
 * Récupère toutes les audiences d'un avocat via ses affaires
 */
@Transactional(readOnly = true)
public List<Audience> getAudiencesParAvocat(String username) {
    // Récupérer toutes les affaires de l'avocat
    List<AffaireJudiciaire> affaires = getAffairesParAvocat(username);
    
    // Collecter toutes les audiences de ces affaires
    return affaires.stream()
        .flatMap(affaire -> affaire.getAudiences().stream())
        .collect(Collectors.toList());
}

/**
 * Récupère les audiences à venir d'un avocat
 */
@Transactional(readOnly = true)
public List<Audience> getAudiencesAVenir(String username) {
    List<AffaireJudiciaire> affaires = getAffairesParAvocat(username);
    LocalDate today = LocalDate.now();
    
    return affaires.stream()
        .flatMap(affaire -> affaire.getAudiences().stream())
        .filter(a -> a.getDateAudience() != null && 
                     a.getDateAudience().isAfter(today) &&
                     !"TENUE".equals(a.getStatut().name()) &&
                     !"ANNULEE".equals(a.getStatut().name()))
        .sorted(Comparator.comparing(Audience::getDateAudience))
        .collect(Collectors.toList());
}

/**
 * Récupère une audience par son ID
 */
@Transactional(readOnly = true)
public Audience getAudienceById(Long audienceId) {
    return audienceRepo.findById(audienceId)
        .orElseThrow(() -> new RuntimeException("Audience introuvable : " + audienceId));
}

/**
 * Modifier une audience
 */
@Transactional
public void modifierAudience(Long audienceId, LocalDate dateAudience, String heure, 
                              String salle, String motif, String observations) {
    Audience audience = getAudienceById(audienceId);
    audience.setDateAudience(dateAudience);
    audience.setHeure(heure);
    audience.setSalle(salle);
    audience.setMotif(motif);
    audienceRepo.save(audience);
}

/**
 * Changer le statut d'une audience
 */
@Transactional
public void changerStatutAudience(Long audienceId, String statut) {
    Audience audience = getAudienceById(audienceId);
    audience.setStatut(Audience.StatutAudience.valueOf(statut));  // ✅ enum    audienceRepo.save(audience);
}

/**
 * Supprimer une audience
 */
@Transactional
public void supprimerAudience(Long audienceId) {
    Audience audience = getAudienceById(audienceId);
    audienceRepo.delete(audience);
}

/**
 * Sauvegarder une audience
 */
@Transactional
public Audience saveAudience(Audience audience) {
    return audienceRepo.save(audience);
}

public void modifierJugement(Long id, String typeJugement, LocalDate localDate, String montantJuge,
        String delaiPaiement, String observations) {
    throw new UnsupportedOperationException("Unimplemented method 'modifierJugement'");
}


}