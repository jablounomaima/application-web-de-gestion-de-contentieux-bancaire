package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.TypeNotification;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AffaireJudiciaireService {

    private final AffaireJudiciaireRepository affaireRepo;
    private final AudienceRepository audienceRepo;
    private final DocumentAffaireRepository documentRepo;
    private final MissionRepository missionRepo;
    private final DossierRepository dossierRepo;    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────
    //  AFFAIRE
    // ─────────────────────────────────────────────

    public AffaireJudiciaire creerAffaire(Long missionId, String tribunal,
                                           String numeroRole, String chambre,
                                           String agentUsername) {
        Mission mission = missionRepo.findById(missionId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        DossierContentieux dossier = mission.getPrestation().getDossier();

        affaireRepo.findByDossierId(dossier.getId()).ifPresent(a -> {
            throw new RuntimeException("Une affaire existe déjà pour ce dossier : " + a.getNumeroAffaire());
        });

        AffaireJudiciaire affaire = new AffaireJudiciaire();
        affaire.setNumeroAffaire(genererNumero());
        affaire.setMission(mission);
        affaire.setDossier(dossier);
        affaire.setTribunal(tribunal);
        affaire.setNumeroRole(numeroRole);
        affaire.setChambre(chambre);
        affaire.setDateLancement(LocalDate.now());
        // Utilise l'enum interne de AffaireJudiciaire
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.EN_COURS);

        affaire = affaireRepo.save(affaire);

        dossier.setStatut(DossierStatus.EN_PROCEDURE);
        dossierRepo.save(dossier);

        // NotificationService avec 5 paramètres (sans l'URL)
        notificationService.notifier(
            mission.getPrestataire().getUsername(),
            "Affaire judiciaire ouverte",
            "L'affaire " + affaire.getNumeroAffaire() + " a été ouverte. Tribunal : " + tribunal,
            TypeNotification.MISSION.name(),
            dossier
        );

        log.info("Affaire {} créée pour le dossier {}", affaire.getNumeroAffaire(), dossier.getNumeroDossier());
        return affaire;
    }

    @Transactional(readOnly = true)
    public AffaireJudiciaire getAffaireById(Long id) {
        return affaireRepo.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<AffaireJudiciaire> getAffairesParAvocat(String username) {
        return affaireRepo.findByAvocatUsername(username);
    }

    @Transactional(readOnly = true)
    public AffaireJudiciaire getAffaireParDossier(Long dossierId) {
        return affaireRepo.findByDossierId(dossierId).orElse(null);
    }

    // ─────────────────────────────────────────────
    //  AUDIENCES
    // ─────────────────────────────────────────────

    public Audience ajouterAudience(Long affaireId, LocalDate dateAudience,
                                     String heure, String salle, String motif) {
        AffaireJudiciaire affaire = affaireRepo.findByIdWithDetails(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable"));

        Audience audience = new Audience();
        audience.setAffaire(affaire);
        audience.setDateAudience(dateAudience);
        audience.setHeure(heure);
        audience.setSalle(salle);
        audience.setMotif(motif);
        audience.setStatut(Audience.StatutAudience.PLANIFIEE);

        affaire.setDateProchainAudience(dateAudience);
        affaireRepo.save(affaire);

        Audience saved = audienceRepo.save(audience);

        // Notification avec 5 paramètres
        notificationService.notifier(
            affaire.getDossier().getAgentCreateur().getUsername(),
            "Audience planifiée",
            "Audience du " + dateAudience.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " pour l'affaire " + affaire.getNumeroAffaire(),
            TypeNotification.MISSION.name(),
            affaire.getDossier()
        );

        return saved;
    }

    public Audience enregistrerResultatAudience(Long audienceId,
                                                  String resultat,
                                                  Audience.StatutAudience statut,
                                                  LocalDate prochaineAudience) {
        Audience audience = audienceRepo.findById(audienceId)
            .orElseThrow(() -> new RuntimeException("Audience introuvable"));

        audience.setResultat(resultat);
        audience.setStatut(statut);
        audience.setProchaineAudience(prochaineAudience);

        if (prochaineAudience != null) {
            AffaireJudiciaire affaire = audience.getAffaire();
            affaire.setDateProchainAudience(prochaineAudience);
            affaireRepo.save(affaire);
        }

        return audienceRepo.save(audience);
    }

    // ─────────────────────────────────────────────
    //  JUGEMENT
    // ─────────────────────────────────────────────

    public AffaireJudiciaire enregistrerJugement(Long affaireId,
                                                   AffaireJudiciaire.TypeJugement typeJugement,
                                                   LocalDate dateJugement,
                                                   String montantJuge,
                                                   String delaiPaiement,
                                                   String description) {
        AffaireJudiciaire affaire = affaireRepo.findByIdWithDetails(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable"));

        affaire.setTypeJugement(typeJugement);
        affaire.setDateJugement(dateJugement);
        affaire.setMontantJuge(montantJuge);
        affaire.setDelaiPaiementJuge(delaiPaiement);
        affaire.setDescriptionJugement(description);
        // Utilise l'enum interne
        affaire.setStatut(AffaireJudiciaire.StatutAffaire.JUGEMENT_RENDU);

        affaire = affaireRepo.save(affaire);

        // Notification avec 5 paramètres
        notificationService.notifier(
            affaire.getDossier().getAgentCreateur().getUsername(),
            "Jugement rendu — " + affaire.getNumeroAffaire(),
            "Jugement " + typeJugement + " rendu le "
                + dateJugement.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ". Montant : " + montantJuge,
            TypeNotification.PV_MISSION.name(),
            affaire.getDossier()
        );

        return affaire;
    }

    // ─────────────────────────────────────────────
    //  DOCUMENTS / UPLOAD
    // ─────────────────────────────────────────────

    public DocumentAffaire uploadDocument(Long affaireId,
                                           MultipartFile file,
                                           DocumentAffaire.TypeDocument typeDocument,
                                           String description,
                                           String uploadeePar) throws IOException {
        AffaireJudiciaire affaire = affaireRepo.findByIdWithDetails(affaireId)
            .orElseThrow(() -> new RuntimeException("Affaire introuvable"));

        String nomServeur = fileStorageService.stocker(file, "affaires");
        String cheminComplet = fileStorageService.getCheminFichier("affaires", nomServeur).toString();

        DocumentAffaire doc = new DocumentAffaire();
        doc.setAffaire(affaire);
        doc.setNomFichierOriginal(file.getOriginalFilename());
        doc.setNomFichierServeur(nomServeur);
        doc.setCheminFichier(cheminComplet);
        doc.setTypeMime(file.getContentType());
        doc.setTailleFichier(file.getSize());
        doc.setTypeDocument(typeDocument);
        doc.setDescription(description);
        doc.setUploadeePar(uploadeePar);

        DocumentAffaire saved = documentRepo.save(doc);

        // Notification avec 5 paramètres
        notificationService.notifier(
            affaire.getDossier().getAgentCreateur().getUsername(),
            "Nouveau document — " + affaire.getNumeroAffaire(),
            "Document ajouté : " + typeDocument + " par " + uploadeePar
                + " (" + doc.getTailleFormatee() + ")",
            TypeNotification.PV_MISSION.name(),
            affaire.getDossier()
        );

        log.info("Document uploadé : {} pour affaire {}", nomServeur, affaire.getNumeroAffaire());
        return saved;
    }

    public void supprimerDocument(Long documentId, String username) {
        DocumentAffaire doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document introuvable"));

        if (!doc.getUploadeePar().equals(username)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce document");
        }

        fileStorageService.supprimer("affaires", doc.getNomFichierServeur());
        documentRepo.delete(doc);
    }

    @Transactional(readOnly = true)
    public DocumentAffaire getDocumentById(Long id) {
        return documentRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Document introuvable"));
    }

    // ─────────────────────────────────────────────
    //  UTILITAIRE
    // ─────────────────────────────────────────────

    private String genererNumero() {
        String annee = String.valueOf(LocalDate.now().getYear());
        String prefix = "AFF-" + annee + "-";
        Integer maxSeq = affaireRepo.findMaxSequence(prefix);
        int seq = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%05d", seq);
    }
}