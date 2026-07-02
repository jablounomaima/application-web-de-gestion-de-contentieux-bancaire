package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.entity.Audience.StatutAudience;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.LocalDate;
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
    private final PrestataireRepository       prestataireRepository;
    private final DossierRepository           dossierRepository;
    private final jakarta.persistence.EntityManager entityManager;

    // ❌ missionRepository retiré : AffaireJudiciaire ne dépend plus de Mission.
    //    Si un autre service (Huissier/Expert) a besoin de MissionRepository,
    //    il l'injecte de son côté — ce service n'en a plus besoin.

    // =========================================================
    //  AFFAIRE
    // =========================================================

    // ─────────────────────────────────────────────
    //  CRÉER AFFAIRE — directement depuis le dossier + avocat choisi
    //  ❌ Ne prend plus un missionId : l'affaire est autonome
    // ─────────────────────────────────────────────
    @Transactional
    public AffaireJudiciaire creerAffaire(Long dossierId, Long avocatId, String username) {

        log.info(">>> CREATION AFFAIRE START - dossierId={} avocatId={} par agent={}",
                dossierId, avocatId, username);

        // ── 1. Charger le dossier ──────────────────────────────
        DossierContentieux dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dossier introuvable : id=" + dossierId));

        // ── 2. Vérification doublon (une affaire par dossier ET par avocat) ──
        boolean alreadyAssignedToThisAvocat = affaireRepo.findByDossier_Id(dossierId).stream()
                .anyMatch(a -> a.getAvocat() != null && a.getAvocat().getId().equals(avocatId));

        if (alreadyAssignedToThisAvocat) {
            throw new IllegalStateException(
                    "Une affaire judiciaire existe déjà pour cet avocat sur ce dossier.");
        }

        // ── 3. Charger l'avocat ─────────────────────────────────
        Prestataire avocat = prestataireRepository.findById(avocatId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Avocat introuvable : id=" + avocatId));

        if (avocat.getType() != TypePrestataire.AVOCAT) {
            throw new IllegalArgumentException("Le prestataire sélectionné n'est pas un avocat.");
        }

        // ── 4. Construire et sauvegarder ─────────────────────────
        AffaireJudiciaire affaire = new AffaireJudiciaire();

        String nouveauNumero = genererNumeroAffaire();
        affaire.setNumeroAffaire(nouveauNumero);

        affaire.setTribunal(null);
        affaire.setNumeroRole(null);
        affaire.setChambre(null);

        affaire.setDateLancement(LocalDate.now());
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
        affaire.setAvocat(avocat);
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
    public List<AffaireJudiciaire> getAllAffairesParDossier(Long dossierId) {
        List<AffaireJudiciaire> affaires = affaireRepo.findByDossier_Id(dossierId);
        // Charger les détails (audiences, documents) pour chaque affaire
        List<AffaireJudiciaire> detailed = new ArrayList<>();
        for (AffaireJudiciaire a : affaires) {
            AffaireJudiciaire full = getAffaireById(a.getId());
            if (full != null) detailed.add(full);
        }
        // Trier par date de lancement (plus récente en premier)
        detailed.sort(Comparator.comparing(AffaireJudiciaire::getDateLancement).reversed());
        return detailed;
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

    @Transactional
    public AffaireJudiciaire reassignerAvocatPourDossier(Long dossierId, Long prestataireId) {
        DossierContentieux dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : id=" + dossierId));

        Prestataire nouveauAvocat = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> new IllegalArgumentException("Avocat introuvable."));

        if (nouveauAvocat.getType() != TypePrestataire.AVOCAT) {
            throw new IllegalArgumentException("Le prestataire sélectionné n'est pas un avocat.");
        }
        if (!nouveauAvocat.isActif()) {
            throw new IllegalArgumentException("Cet avocat n'est pas actif.");
        }

        // Vérifier si ce nouvel avocat a déjà une affaire sur ce dossier
        boolean dejaAssigne = affaireRepo.findByDossier_Id(dossierId).stream()
                .anyMatch(a -> a.getAvocat() != null && a.getAvocat().getId().equals(nouveauAvocat.getId()));

        if (dejaAssigne) {
             throw new IllegalStateException("Cet avocat est déjà assigné à ce dossier.");
        }

        // Créer une nouvelle affaire pour le nouvel avocat
        AffaireJudiciaire affaire = new AffaireJudiciaire();
        affaire.setNumeroAffaire(genererNumeroAffaire());
        affaire.setDateLancement(LocalDate.now());
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
        affaire.setAvocat(nouveauAvocat);
        affaire.setDossier(dossier);

        return affaireRepo.save(affaire);
    }

    // ─────────────────────────────────────────────
    //  AFFAIRES PAR AVOCAT — simplifié
    //  ❌ Ne fusionne plus 3 sources (viaMission/viaAvocat/viaDossier) :
    //     l'avocat est maintenant l'unique relation directe sur l'affaire.
    // ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AffaireJudiciaire> getAffairesParAvocat(String username) {
        List<AffaireJudiciaire> affaires = affaireRepo.findByAvocatUsernameWithMission(username);
        log.info("getAffairesParAvocat('{}') → {} affaire(s)", username, affaires.size());
        return affaires;
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

    // ─────────────────────────────────────────────────────────────
    //  AUDIENCES — surcharge complète (appelée par le controller)
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

    public void modifierStatutAudience(Long audienceId,
                                        StatutAudience statut,
                                        String resultat,
                                        LocalDate prochaineAudience) {

        enregistrerResultatAudience(audienceId, resultat, statut, prochaineAudience);
    }

    public AffaireJudiciaire enregistrerJugement(Long affaireId,
                                                   AffaireJudiciaire.TypeJugement typeJugement,
                                                   LocalDate dateJugement,
                                                   String montantJuge,
                                                   String delaiPaiement,
                                                   String description) {

        return enregistrerJugement(
                affaireId,
                typeJugement.name(),
                dateJugement,
                montantJuge,
                delaiPaiement,
                description
        );
    }

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
    //  ❌ Ne prend plus une Mission : prend l'avocat directement
    // ─────────────────────────────────────────────
    @Transactional
    public AffaireJudiciaire creerAffaireDirecte(Prestataire avocat,
                                                  DossierContentieux dossier,
                                                  String username) {

        log.info(">>> creerAffaireDirecte dossier={} avocat={}",
                dossier.getNumeroDossier(),
                avocat != null ? avocat.getUsername() : "NULL");

        // Vérification doublon (une affaire par dossier ET par avocat)
        boolean alreadyAssignedToThisAvocat = affaireRepo.findByDossier_Id(dossier.getId()).stream()
                .anyMatch(a -> a.getAvocat() != null && a.getAvocat().getId().equals(avocat.getId()));

        if (alreadyAssignedToThisAvocat) {
            throw new IllegalStateException(
                    "Une affaire judiciaire existe déjà pour cet avocat sur ce dossier.");
        }

        if (avocat == null) {
            throw new IllegalStateException("Aucun avocat fourni pour créer l'affaire.");
        }

        AffaireJudiciaire affaire = new AffaireJudiciaire();
        affaire.setNumeroAffaire(genererNumeroAffaire());
        affaire.setDateLancement(LocalDate.now());
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);
        affaire.setAvocat(avocat);
        affaire.setDossier(dossier);

        AffaireJudiciaire result = affaireRepo.save(affaire);

        log.info(">>> AFFAIRE CRÉÉE : id={} num={}",
                result.getId(), result.getNumeroAffaire());

        return result;
    }

    // ❌ findByMissionId(Long) supprimée : n'a plus de sens sans relation Mission

    // =========================================================
    //  📄 PV — Soumission par l'avocat (directement sur l'affaire)
    // =========================================================
    @Transactional
    public void soumettreAvocatPV(Long affaireId, String pvTexte) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        affaire.setPvTexte(pvTexte);
        affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);
        affaireRepo.save(affaire);
        // ❌ Plus de synchronisation avec Mission : le statut PV vit uniquement
        //    sur l'affaire (affaire.pvStatut).
    }

    // ─────────────────────────────────────────────
    // PV avec fichiers
    // ─────────────────────────────────────────────
    @Transactional
    public void soumettreAvocatPVAvecFichiers(Long affaireId,
                                               String pvTexte,
                                               List<MultipartFile> fichiers) throws IOException {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        affaire.setPvTexte(pvTexte);
        affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);

        List<String> fichiersExistants = new ArrayList<>(
            affaire.getPvFichiers() != null ? affaire.getPvFichiers() : new ArrayList<>()
        );

        if (fichiers != null && !fichiers.isEmpty()) {
            for (MultipartFile f : fichiers) {
                if (f.isEmpty()) continue;

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
        affaireRepo.save(affaire);
    }

    // =========================================================
    //  🧾 FACTURE — Soumission par l'avocat (directement sur l'affaire)
    // =========================================================
    @Transactional
    public void soumettreAvocatFacture(Long affaireId, String factureRef, Double montant) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));

        affaire.setFactureRef(factureRef);
        affaire.setMontantFacture(montant);
        affaire.setFactureStatut(AffaireJudiciaire.StatutFacture.EN_ATTENTE_VALIDATION);
        affaireRepo.save(affaire);
    }

    public AffaireJudiciaire findById(Long id) {
        return affaireRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + id));
    }

    // ─────────────────────────────────────────────
    //  PV — modifier / supprimer (directement sur l'affaire)
    //  ❌ N'écrit plus sur Mission : utilise les champs propres à l'affaire
    // ─────────────────────────────────────────────
    @Transactional
    public void modifierAvocatPV(Long affaireId, String pvTexte) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
        affaire.setPvTexte(pvTexte);
        affaire.setPvStatut(AffaireJudiciaire.StatutPV.EN_ATTENTE);
        affaireRepo.save(affaire);
    }

    @Transactional
    public void supprimerAvocatPV(Long affaireId) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
        affaire.setPvTexte(null);
        affaire.setPvStatut(null);
        affaire.setPvFichiers(new ArrayList<>());
        affaireRepo.save(affaire);
    }

    // ─────────────────────────────────────────────
    //  FACTURE — modifier / supprimer (directement sur l'affaire)
    // ─────────────────────────────────────────────
    @Transactional
    public void modifierAvocatFacture(Long affaireId, String factureRef, Double montant) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
        affaire.setFactureRef(factureRef);
        affaire.setMontantFacture(montant);
        affaireRepo.save(affaire);
    }

    @Transactional
    public void supprimerAvocatFacture(Long affaireId) {
        AffaireJudiciaire affaire = affaireRepo.findById(affaireId)
                .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + affaireId));
        affaire.setFactureRef(null);
        affaire.setMontantFacture(null);
        affaire.setFactureStatut(null);
        affaireRepo.save(affaire);
    }

    @Transactional
    public void validerPV(Long affaireId, boolean accepte) {
        AffaireJudiciaire affaire = findById(affaireId);
        affaire.setPvStatut(accepte ? AffaireJudiciaire.StatutPV.VALIDE : AffaireJudiciaire.StatutPV.REFUSE);
        affaireRepo.save(affaire);
    }

    @Transactional
    public void validerFacture(Long affaireId, boolean accepte) {
        AffaireJudiciaire affaire = findById(affaireId);
        affaire.setFactureStatut(accepte ? AffaireJudiciaire.StatutFacture.PAYEE : AffaireJudiciaire.StatutFacture.REJETEE);
        affaireRepo.save(affaire);
    }

    // ✅ Validation par le validateur financier avec commentaire
    @Transactional
    public void validerFactureParValidateur(Long affaireId, boolean accepte,
                                            String commentaire, String validePar) {
        AffaireJudiciaire affaire = findById(affaireId);
        affaire.setFactureStatut(accepte
                ? AffaireJudiciaire.StatutFacture.PAYEE
                : AffaireJudiciaire.StatutFacture.REJETEE);
        affaire.setFactureCommentaireValidation(commentaire);
        affaire.setFactureValidePar(validePar);
        affaireRepo.save(affaire);
    }

    @Transactional
    public AffaireJudiciaire sauvegarderAffaire(AffaireJudiciaire affaire) {
        return affaireRepo.save(affaire);
    }
}