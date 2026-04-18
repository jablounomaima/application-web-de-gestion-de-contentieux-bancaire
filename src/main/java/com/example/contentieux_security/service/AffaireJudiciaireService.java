package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AffaireJudiciaireService {

    private final AffaireJudiciaireRepository affaireRepo;
    private final AudienceRepository          audienceRepo;
    private final DocumentAffaireRepository   documentRepo;
    private final MissionRepository           missionRepository;

    // =========================================================
    //  AFFAIRE
    // =========================================================

    public AffaireJudiciaire creerAffaire(Long missionId,
                                           String tribunal,
                                           String numeroRole,
                                           String chambre,
                                           String username) {

        System.out.println(">>> CREATION AFFAIRE START - missionId=" + missionId);

        // 1. Charger la mission avec tous ses détails (fetch join)
        Mission mission = missionRepository.findByIdWithDetails(missionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mission introuvable : id=" + missionId));

        System.out.println(">>> Mission trouvée : " + mission.getNumeroMission());
        System.out.println(">>> Prestataire : " + (mission.getPrestataire() != null ? mission.getPrestataire().getUsername() : "NULL"));
        System.out.println(">>> Prestation : " + (mission.getPrestation() != null ? mission.getPrestation().getId() : "NULL"));
        System.out.println(">>> Dossier : " + (mission.getPrestation() != null && mission.getPrestation().getDossier() != null ? mission.getPrestation().getDossier().getId() : "NULL"));

        // 2. Vérification doublon
        affaireRepo.findByMission_Id(missionId).ifPresent(a -> {
            throw new IllegalStateException(
                    "Une affaire existe déjà pour cette mission : " + a.getNumeroAffaire());
        });

        // 3. Vérification que le dossier est accessible
        if (mission.getPrestation() == null) {
            throw new IllegalStateException(
                    "La mission " + missionId + " n'a pas de prestation liée.");
        }
        if (mission.getPrestation().getDossier() == null) {
            throw new IllegalStateException(
                    "La prestation liée à la mission " + missionId + " n'a pas de dossier.");
        }

        // 4. Construction de l'affaire
        AffaireJudiciaire affaire = new AffaireJudiciaire();
        affaire.setNumeroAffaire(genererNumeroAffaire());
        affaire.setTribunal(tribunal);
        affaire.setNumeroRole(numeroRole);
        affaire.setChambre(chambre);
        affaire.setDateLancement(LocalDate.now());
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
        affaire.setAvocat(mission.getPrestataire());
        affaire.setMission(mission);
        affaire.setDossier(mission.getPrestation().getDossier());

        System.out.println(">>> AVANT SAVE : " + affaire.getNumeroAffaire());

        // 5. Sauvegarde
        AffaireJudiciaire result = affaireRepo.save(affaire);

        System.out.println(">>> APRES SAVE : id=" + result.getId());

        return result;
    }

    @Transactional(readOnly = true)
    public AffaireJudiciaire getAffaireParDossier(Long dossierId) {
        return affaireRepo.findByDossier_Id(dossierId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AffaireJudiciaire getAffaireById(Long id) {
        // Charger avec audiences d'abord
        AffaireJudiciaire affaire = affaireRepo.findByIdWithAudiences(id).orElse(null);
        if (affaire == null) return null;
        // Puis initialiser les documents séparément
        affaireRepo.findByIdWithDocuments(id)
                   .ifPresent(a -> affaire.setDocuments(a.getDocuments()));
        return affaire;
    }

    public List<AffaireJudiciaire> getAffairesParAvocat(String username) {
        List<AffaireJudiciaire> viaMission = affaireRepo.findByMissionPrestataireUsernameWithDetails(username);
        List<AffaireJudiciaire> viaAvocat  = affaireRepo.findByAvocatUsernameWithDetails(username);
    
        return Stream.concat(viaMission.stream(), viaAvocat.stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private String genererNumeroAffaire() {
        long count = affaireRepo.count() + 1;
        return String.format("AFF-%d-%05d", LocalDate.now().getYear(), count);
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

    public Audience enregistrerResultatAudience(Long audienceId,
                                                 String resultat,
                                                 StatutAudience statut,
                                                 LocalDate prochaineAudience) {
        Audience audience = audienceRepo.findById(audienceId).orElse(null);
        if (audience == null) return null;

        audience.setResultat(resultat);
        audience.setStatut(statut);

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

    @Transactional(readOnly = true)
    public List<Audience> getAudiencesParAvocat(String username) {
        List<AffaireJudiciaire> affaires = getAffairesParAvocat(username);
        return affaires.stream()
                .flatMap(a -> a.getAudiences().stream())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Audience> getAudiencesAVenir(String username) {
        LocalDate today = LocalDate.now();
        return getAudiencesParAvocat(username).stream()
                .filter(a -> a.getDateAudience() != null
                        && a.getDateAudience().isAfter(today)
                        && a.getStatut() != StatutAudience.TENUE
                        && a.getStatut() != StatutAudience.ANNULEE)
                .sorted(Comparator.comparing(Audience::getDateAudience))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Audience getAudienceById(Long audienceId) {
        return audienceRepo.findById(audienceId)
                .orElseThrow(() -> new RuntimeException("Audience introuvable : " + audienceId));
    }

    public void modifierAudience(Long audienceId, LocalDate dateAudience, String heure,
                                  String salle, String motif, String observations) {
        Audience audience = getAudienceById(audienceId);
        audience.setDateAudience(dateAudience);
        audience.setHeure(heure);
        audience.setSalle(salle);
        audience.setMotif(motif);
        audienceRepo.save(audience);
    }

    public void changerStatutAudience(Long audienceId, String statut) {
        Audience audience = getAudienceById(audienceId);
        audience.setStatut(StatutAudience.valueOf(statut));
        audienceRepo.save(audience);
    }

    public void supprimerAudience(Long audienceId) {
        audienceRepo.delete(getAudienceById(audienceId));
    }

    public Audience saveAudience(Audience audience) {
        return audienceRepo.save(audience);
    }

    // =========================================================
    //  JUGEMENT
    // =========================================================

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

    public void modifierJugement(Long id, String typeJugement, LocalDate dateJugement,
                                  String montantJuge, String delaiPaiement, String observations) {
        throw new UnsupportedOperationException("Unimplemented method 'modifierJugement'");
    }

    // =========================================================
    //  TRIBUNAL
    // =========================================================

    @Transactional
    public AffaireJudiciaire modifierTribunal(Long affaireId,
                                               String tribunal,
                                               String chambre,
                                               String numeroRole) {
 
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Affaire introuvable : " + affaireId));
 
        affaire.setTribunal(tribunal);
        affaire.setChambre(chambre);
        affaire.setNumeroRole(numeroRole);
 
        return affaireRepo.save(affaire);
    }

    // =========================================================
    //  DOCUMENTS
    // =========================================================

    public DocumentAffaire uploadDocument(Long affaireId,
                                           MultipartFile file,
                                           DocumentAffaire.TypeDocument typeDocument,
                                           String description,
                                           String uploadeePar) throws IOException {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);

        DocumentAffaire doc = new DocumentAffaire();
        doc.setNomFichierOriginal(file.getOriginalFilename());
        doc.setNomFichierServeur(file.getOriginalFilename());
        doc.setCheminFichier("");
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



// ══════════════════════════════════════════════════════════════════
//  MÉTHODES À AJOUTER à la fin de AffaireJudiciaireService
//  ✅ Aucune modification du code existant
//  ✅ Utilise audienceRepo et affaireRepo déjà injectés
// ══════════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    //  AUDIENCES — surcharge complète (appelée par le controller)
    //  Complète la version existante ajouterAudience(5 params)
    //  avec resultat + prochaineAudience + statut
    // ─────────────────────────────────────────────────────────────

    public Audience ajouterAudience(Long affaireId,
                                     LocalDate dateAudience,
                                     String heure,
                                     String salle,
                                     String motif,
                                     String resultat,
                                     LocalDate prochaineAudience,
                                     StatutAudience statut) {

        AffaireJudiciaire affaire = affaireRepo.findById(affaireId).orElse(null);
        if (affaire == null) return null;

        Audience audience = new Audience();
        audience.setDateAudience(dateAudience);
        audience.setHeure(heure);
        audience.setSalle(salle);
        audience.setMotif(motif);
        audience.setResultat(resultat);
        audience.setProchaineAudience(prochaineAudience);
        audience.setStatut(statut != null ? statut : StatutAudience.PLANIFIEE);
        audience.setAffaire(affaire);

        if (prochaineAudience != null) {
            affaire.setDateProchainAudience(prochaineAudience);
            affaireRepo.save(affaire);
        }

        return audienceRepo.save(audience);
    }

    // ─────────────────────────────────────────────────────────────
    //  AUDIENCES — modifier statut + résultat + prochaine audience
    //  Wrapper propre autour de enregistrerResultatAudience
    //  + changerStatutAudience existants
    // ─────────────────────────────────────────────────────────────

    public void modifierStatutAudience(Long audienceId,
                                        StatutAudience statut,
                                        String resultat,
                                        LocalDate prochaineAudience) {

        // Réutilise enregistrerResultatAudience (déjà écrit, gère la cascade sur l'affaire)
        enregistrerResultatAudience(audienceId, resultat, statut, prochaineAudience);
    }

    // ─────────────────────────────────────────────────────────────
    //  JUGEMENT — surcharge avec TypeJugement enum (type-safe)
    //  La version existante prend un String — celle-ci prend l'enum
    //  directement, évite le try/catch valueOf dans le controller
    // ─────────────────────────────────────────────────────────────

    public AffaireJudiciaire enregistrerJugement(Long affaireId,
                                                   AffaireJudiciaire.TypeJugement typeJugement,
                                                   LocalDate dateJugement,
                                                   String montantJuge,
                                                   String delaiPaiement,
                                                   String description) {

        // Délègue à la version String existante — pas de duplication de logique
        return enregistrerJugement(
                affaireId,
                typeJugement.name(),   // String attendu par la méthode existante
                dateJugement,
                montantJuge,
                delaiPaiement,
                description
        );
    }


// ─────────────────────────────────────────────
//  AUDIENCES — modifier complète
// ─────────────────────────────────────────────
@Transactional
public void modifierAudienceComplete(Long audienceId,
                                      LocalDate dateAudience,
                                      String heure,
                                      String salle,
                                      String motif,
                                      String resultat,
                                      LocalDate prochaineAudience,
                                      StatutAudience statut) {
    Audience audience = audienceRepo.findById(audienceId)
            .orElseThrow(() -> new RuntimeException("Audience introuvable : " + audienceId));

    audience.setDateAudience(dateAudience);
    audience.setHeure(heure);
    audience.setSalle(salle);
    audience.setMotif(motif);
    audience.setResultat(resultat);
    audience.setStatut(statut != null ? statut : StatutAudience.PLANIFIEE);
    audience.setProchaineAudience(prochaineAudience);

    // Mettre à jour la prochaine audience sur l'affaire
    if (prochaineAudience != null) {
        AffaireJudiciaire affaire = audience.getAffaire();
        if (affaire != null) {
            affaire.setDateProchainAudience(prochaineAudience);
            affaireRepo.save(affaire);
        }
    }
    audienceRepo.save(audience);
}

// ─────────────────────────────────────────────
//  JUGEMENT — supprimer
// ─────────────────────────────────────────────
@Transactional
public void supprimerJugement(Long affaireId) {
    AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

    affaire.setTypeJugement(null);
    affaire.setDateJugement(null);
    affaire.setMontantJuge(null);
    affaire.setDelaiPaiementJuge(null);
    affaire.setDescriptionJugement(null);
    affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
    affaireRepo.save(affaire);
}




}