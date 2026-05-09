package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.enums.StatutMission;
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
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Base64;
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AffaireJudiciaireService {

    private final AffaireJudiciaireRepository affaireRepo;
    private final AudienceRepository          audienceRepo;
    private final DocumentAffaireRepository   documentRepo;
    private final MissionRepository           missionRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final AffaireJudiciaireRepository affaireJudiciaireRepository;
    // =========================================================
    //  AFFAIRE
    // =========================================================
// Ajouter dans AffaireJudiciaireService

@Transactional
public AffaireJudiciaire creerAffaire(Long missionId, String username) { // Na77ina el 3 paramètres mel signature

    log.info(">>> CREATION AFFAIRE START - missionId={} par agent={}", missionId, username);

    // ── 1. Charger la mission avec TOUS ses détails ──────────
    Mission mission = missionRepository.findByIdWithDetails(missionId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "Mission introuvable : id=" + missionId));

    // ── 2. Vérification doublon ──────────────────────────────
    if (affaireRepo.findByMission_Id(missionId).isPresent()) {
        throw new IllegalStateException(
                "Une affaire existe déjà pour cette mission.");
    }

    // ── 3. Récupérer le dossier ──────────────────────────────
    DossierContentieux dossier = null;

    if (mission.getPrestation() != null && mission.getPrestation().getDossier() != null) {
        dossier = mission.getPrestation().getDossier();
    }

    if (dossier == null) {
        dossier = missionRepository.findDossierByMissionId(missionId);
    }

    if (dossier == null) {
        throw new IllegalStateException(
                "Impossible de trouver le dossier pour la mission " + missionId);
    }

    // ── 4. Récupérer l'avocat ────────────────────────────────
    Prestataire avocat = mission.getPrestataire();
    if (avocat == null) {
        throw new IllegalStateException(
                "La mission " + missionId + " n'a pas d'avocat assigné.");
    }

    // ── 5. Construire et sauvegarder ─────────────────────────
    AffaireJudiciaire affaire = new AffaireJudiciaire();
    
    // El "numeroAffaire" iji automatique
    String nouveauNumero = genererNumeroAffaire();
    affaire.setNumeroAffaire(nouveauNumero);
    
    // Houni tna77ina el variables, donc iwalliw null fil base (cella ken t7ebhom ferghin)
    affaire.setTribunal(null);
    affaire.setNumeroRole(null);
    affaire.setChambre(null);
    
    affaire.setDateLancement(LocalDate.now());
    affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
    affaire.setAvocat(avocat);
    affaire.setMission(mission);
    affaire.setDossier(dossier);

    AffaireJudiciaire result = affaireRepo.save(affaire);
    entityManager.flush(); 

    log.info(">>> AFFAIRE SAUVEGARDÉE : id={} num={} dossier={}",
            result.getId(),
            result.getNumeroAffaire(),
            result.getDossier().getNumeroDossier());

    return result;
}
    
    @Transactional(readOnly = true)
    public AffaireJudiciaire getAffaireParDossier(Long dossierId) {
        List<AffaireJudiciaire> affaires = affaireRepo.findByDossier_Id(dossierId);
        if (affaires.isEmpty()) return null;
        // Retourne la plus récente
        return affaires.stream()
                .max(Comparator.comparing(AffaireJudiciaire::getDateLancement))
                .orElse(null);
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

    @Transactional(readOnly = true)
    public List<AffaireJudiciaire> getAffairesParAvocat(String username) {
    
        List<AffaireJudiciaire> viaMission =
                affaireRepo.findByMissionPrestataireUsernameWithMission(username);
        List<AffaireJudiciaire> viaAvocat =
                affaireRepo.findByAvocatUsernameWithMission(username);
        List<AffaireJudiciaire> viaDossier =
                affaireRepo.findByDossierMissionPrestataireUsername(username);
    
        log.info("getAffairesParAvocat('{}') → viaMission={} viaAvocat={} viaDossier={}",
                username, viaMission.size(), viaAvocat.size(), viaDossier.size());
    
        List<AffaireJudiciaire> toutes = new java.util.ArrayList<>();
        toutes.addAll(viaMission);
        toutes.addAll(viaAvocat);
        toutes.addAll(viaDossier);
    
        // Dédupliquer par ID
        return toutes.stream()
                .filter(a -> a != null && a.getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getId(),
                        a -> a,
                        (a1, a2) -> a1
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }
    
    
    
    
    private String genererNumeroAffaire() {
        long count = affaireRepo.count() + 1;
        String numero = String.format("AFF-%d-%05d", LocalDate.now().getYear(), count);
        
        int tentatives = 0;
        while (affaireRepo.existsByNumeroAffaire(numero) && tentatives < 100) {
            count++;
            numero = String.format("AFF-%d-%05d", LocalDate.now().getYear(), count);
            tentatives++;
        }
        
        return numero;
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



// ─────────────────────────────────────────────
//  CRÉER AFFAIRE DIRECTE (sans rechargement)
// ─────────────────────────────────────────────
@Transactional
public AffaireJudiciaire creerAffaireDirecte(Mission mission,
                                              DossierContentieux dossier,
                                              String username) {

    log.info(">>> creerAffaireDirecte mission={} dossier={} avocat={}",
            mission.getNumeroMission(),
            dossier.getNumeroDossier(),
            mission.getPrestataire() != null
                    ? mission.getPrestataire().getUsername() : "NULL");

    // Vérification doublon
    if (affaireRepo.findByMission_Id(mission.getId()).isPresent()) {
        throw new IllegalStateException(
                "Une affaire existe déjà pour cette mission.");
    }

    Prestataire avocat = mission.getPrestataire();
    if (avocat == null) {
        throw new IllegalStateException("Pas d'avocat sur cette mission.");
    }

    AffaireJudiciaire affaire = new AffaireJudiciaire();
    affaire.setNumeroAffaire(genererNumeroAffaire());
    affaire.setDateLancement(LocalDate.now());
    affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
    affaire.setAvocat(avocat);
    affaire.setMission(mission);
    affaire.setDossier(dossier);

    AffaireJudiciaire result = affaireRepo.save(affaire);

    log.info(">>> AFFAIRE CRÉÉE : id={} num={}",
            result.getId(), result.getNumeroAffaire());

    return result;
}


public AffaireJudiciaire findByMissionId(Long missionId) {
    return affaireJudiciaireRepository.findByMissionId(missionId)
            .orElse(null);
}



// =========================================================
//  📄 PV — Soumission par l'avocat
// =========================================================
@Transactional
public void soumettreAvocatPV(Long affaireId, String pvTexte) {
    AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

    // ✅ Stocker directement dans l'affaire
    affaire.setPvTexte(pvTexte);
    affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);
    affaireJudiciaireRepository.save(affaire);

    // ✅ Mettre à jour le statut mission si elle existe
    if (affaire.getMission() != null) {
        affaire.getMission().setStatut(StatutMission.PV_SOUMIS);
        missionRepository.save(affaire.getMission());
    }
}

// ─────────────────────────────────────────────
// PV avec fichiers
// ─────────────────────────────────────────────
@Transactional
public void soumettreAvocatPVAvecFichiers(Long affaireId,
                                           String pvTexte,
                                           List<MultipartFile> fichiers) throws IOException {
    AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

    // ✅ Texte PV
    affaire.setPvTexte(pvTexte);
    affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);

    // ✅ Récupérer les fichiers existants et AJOUTER les nouveaux
    List<String> fichiersExistants = new ArrayList<>(
        affaire.getPvFichiers() != null ? affaire.getPvFichiers() : new ArrayList<>()
    );

    if (fichiers != null && !fichiers.isEmpty()) {
        for (MultipartFile f : fichiers) {
            if (f.isEmpty()) continue;

            // ✅ Vérifier doublon par nom
            String nomFichier = f.getOriginalFilename();
            boolean dejaExiste = fichiersExistants.stream()
                    .anyMatch(data -> data.startsWith(nomFichier + "|"));

            if (!dejaExiste) {
                String base64 = Base64.getEncoder().encodeToString(f.getBytes());
                fichiersExistants.add(nomFichier + "|" + f.getContentType() + "|" + base64);
                log.info("Fichier ajouté: {} taille={}", nomFichier, f.getSize());
            } else {
                log.info("Fichier ignoré (doublon): {}", nomFichier);
            }
        }
    }

    affaire.setPvFichiers(fichiersExistants);
    affaireJudiciaireRepository.save(affaire);

    // ✅ Mettre à jour statut mission
    if (affaire.getMission() != null) {
        affaire.getMission().setStatut(StatutMission.PV_SOUMIS);
        missionRepository.save(affaire.getMission());
    }
}

// =========================================================
//  🧾 FACTURE — Soumission par l'avocat
// =========================================================
// ─────────────────────────────────────────────
// Facture
// ─────────────────────────────────────────────
@Transactional
public void soumettreAvocatFacture(Long affaireId, String factureRef, Double montant) {
    AffaireJudiciaire affaire = affaireJudiciaireRepository.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

    // ✅ Stocker directement dans l'affaire
    affaire.setFactureRef(factureRef);
    affaire.setMontantFacture(montant);
    affaire.setFactureStatut(AffaireJudiciaire.StatutFacture.EN_ATTENTE);
    affaireJudiciaireRepository.save(affaire);

    // ✅ Mettre à jour le statut mission si elle existe
    if (affaire.getMission() != null) {
        affaire.getMission().setStatut(StatutMission.FACTURE_SOUMISE);
        missionRepository.save(affaire.getMission());
    }
}

public AffaireJudiciaire findById(Long id) {
    return affaireRepo.findByIdWithMission(id)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + id));
}


@Transactional
public void modifierAvocatPV(Long affaireId, String pvTexte) {
    AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
    Mission mission = affaire.getMission();
    if (mission == null) throw new RuntimeException("Aucune mission liée.");
    mission.setPvMission(pvTexte);
    missionRepository.save(mission);
}

@Transactional
public void supprimerAvocatPV(Long affaireId) {
    AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
    Mission mission = affaire.getMission();
    if (mission == null) throw new RuntimeException("Aucune mission liée.");
    mission.setPvMission(null);
    mission.setStatut(StatutMission.EN_COURS);
    mission.setDateValidationPv(null);
    missionRepository.save(mission);
}

@Transactional
public void modifierAvocatFacture(Long affaireId, String factureRef, Double montant) {
    AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
    Mission mission = affaire.getMission();
    if (mission == null) throw new RuntimeException("Aucune mission liée.");
    mission.setFactureRef(factureRef);
    mission.setMontantFacture(montant);
    missionRepository.save(mission);
}

@Transactional
public void supprimerAvocatFacture(Long affaireId) {
    AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
    Mission mission = affaire.getMission();
    if (mission == null) throw new RuntimeException("Aucune mission liée.");
    mission.setFactureRef(null);
    mission.setMontantFacture(null);
    mission.setStatut(StatutMission.PV_SOUMIS);
    mission.setDateValidationFacture(null);
    missionRepository.save(mission);
}


@Transactional
public AffaireJudiciaire sauvegarderAffaire(AffaireJudiciaire affaire) {
    return affaireJudiciaireRepository.save(affaire);
}
}