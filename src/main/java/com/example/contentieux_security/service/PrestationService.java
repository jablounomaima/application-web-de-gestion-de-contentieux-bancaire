package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.enums.StatutPrestation;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrestationService {

    private final PrestationRepository      prestationRepository;
    private final MissionRepository          missionRepository;
    private final DossierRepository          dossierRepository;
    private final PrestataireRepository      prestataireRepository;
    private final HistoriqueService          historiqueService;
    private final NotificationService        notificationService;
    private final AgentBancaireRepository    agentBancaireRepository;
    private final ResultatMissionRepository  resultatMissionRepository;   // ✅ Ajouté
    private final FichierResultatRepository  fichierResultatRepository;   // ✅ Ajouté

    @Value("${app.upload.dir}")                                            // ✅ Ajouté
    private String uploadDir;

    // ═════════ PRESTATION ═════════

    @Transactional(readOnly = true)
    public Prestation getPrestationById(Long id) {
        return prestationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prestation introuvable : id=" + id));
    }

    @Transactional
    public Prestation lancerPrestation(Long dossierId,
                                       TypePrestation type,
                                       String description,
                                       String agentUsername) {

        // 1. Récupération dossier
        DossierContentieux dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : id=" + dossierId));

        // 2. Récupération agent
        AgentBancaire agent = agentBancaireRepository.findByUsername(agentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Agent introuvable : " + agentUsername));

        // 3. Vérification métier
        if (type == TypePrestation.PROCEDURE_JUDICIAIRE
                && dossier.getStatut() != DossierStatus.VALIDE) {
            throw new IllegalStateException(
                    "Le dossier doit être au statut VALIDE pour lancer une procédure judiciaire. "
                            + "Statut actuel : " + dossier.getStatut());
        }

        // 4. Création prestation
        Prestation prestation = Prestation.builder()
                .numeroPrestation(genererNumeroPrestation())
                .type(type)
                .statut(StatutPrestation.EN_COURS)
                .description(description)
                .dossier(dossier)
                .agentCreateur(agent)
                .dateCreation(LocalDateTime.now())
                .build();

        prestation = prestationRepository.save(prestation);

        // 5. Mise à jour dossier
        if (type == TypePrestation.PROCEDURE_JUDICIAIRE) {
            dossier.setStatut(DossierStatus.EN_PROCEDURE);
        }
        dossierRepository.save(dossier);

        // 6. Historique (protégé)
        try {
            historiqueService.enregistrer(
                    dossier,
                    "PRESTATION_LANCEE",
                    "Prestation lancée : " + type.getLibelle(),
                    agentUsername
            );
        } catch (Exception e) {
            System.err.println("Erreur lors de l'enregistrement de l'historique : " + e.getMessage());
        }

        return prestation;
    }

    // ═════════ MISSION ═════════

    @Transactional
    public Mission designerPrestataire(Long prestationId,
                                       Long prestataireId,
                                       String description,
                                       LocalDate dateFinPrevue,
                                       String agentUsername) {

        Prestation prestation = getPrestationById(prestationId);

        Prestataire prestataire = prestataireRepository.findById(prestataireId)
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable : id=" + prestataireId));

        if (!prestataire.isActif()) {
            throw new IllegalStateException(
                    "Le prestataire " + prestataire.getNom() + " n'est plus actif.");
        }

        validerCompatibiliteTypePrestataire(prestation.getType(), prestataire);

        Mission mission = Mission.builder()
                .numeroMission(genererNumeroMission())
                .description(description)
                .statut(StatutMission.ASSIGNEE)
                .prestation(prestation)
                .prestataire(prestataire)
                .dateAssignation(LocalDate.now())
                .dateFinPrevue(dateFinPrevue)
                .build();

        mission = missionRepository.save(mission);

        notificationService.notifier(
                prestataire.getUsername(),
                "Nouvelle mission assignée",
                "Une mission vous a été assignée pour le dossier "
                        + prestation.getDossier().getNumeroDossier()
                        + " — Mission : " + mission.getNumeroMission(),
                "MISSION",
                prestation.getDossier()
        );

        historiqueService.enregistrer(
                prestation.getDossier(),
                "MISSION_ASSIGNEE",
                prestataire.getType().name() + " désigné : "
                        + prestataire.getNom() + " " + prestataire.getPrenom(),
                agentUsername
        );

        return mission;
    }

    @Transactional(readOnly = true)                                        // ✅ Ajouté
    public List<Mission> getMissionsByPrestation(Long prestationId) {
        getPrestationById(prestationId);
        return missionRepository.findByPrestation_Id(prestationId);
    }

    @Transactional(readOnly = true)
    public List<Mission> getMissionsPrestataire(String username) {
        List<Mission> missions = missionRepository.findByPrestataire_Username(username);

        missions.forEach(m -> {
            if (m.getPrestation() != null &&
                    m.getPrestation().getDossier() != null &&
                    m.getPrestation().getDossier().getClient() != null) {
                m.getPrestation().getDossier().getClient().getNom();
                m.getPrestation().getDossier().getClient().getPrenom();
            }
        });

        return missions;
    }

    @Transactional(readOnly = true)
    public Mission getMissionById(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : id=" + id));
    }

    @Transactional
    public void soumettrePV(Long missionId, String pvTexte, String username) {

        Mission mission = getMissionById(missionId);

        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à soumettre le PV de cette mission.");
        }

        if (mission.getStatut() != StatutMission.ASSIGNEE
                && mission.getStatut() != StatutMission.EN_COURS) {
            throw new IllegalStateException(
                    "Impossible de soumettre un PV pour une mission au statut : " + mission.getStatut());
        }

        if (pvTexte == null || pvTexte.isBlank()) {
            throw new IllegalArgumentException("Le texte du PV ne peut pas être vide.");
        }

        mission.setPvMission(pvTexte);
        mission.setStatut(StatutMission.PV_SOUMIS);
        missionRepository.save(mission);

        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "PV de mission soumis — " + mission.getNumeroMission(),
                "Le prestataire " + mission.getPrestataire().getNom()
                        + " a soumis un PV pour le dossier "
                        + mission.getPrestation().getDossier().getNumeroDossier(),
                "PV_MISSION",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(
                mission.getPrestation().getDossier(),
                "PV_SOUMIS",
                "PV soumis par " + username + " — mission " + mission.getNumeroMission(),
                username
        );
    }

    @Transactional
    public void soumettreFacture(Long missionId,
                                 Double montant,
                                 String factureRef,
                                 String username) {

        Mission mission = getMissionById(missionId);

        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new IllegalStateException("Vous n'êtes pas autorisé à soumettre la facture de cette mission.");
        }

        if (mission.getStatut() != StatutMission.PV_SOUMIS) {
            throw new IllegalStateException(
                    "La facture ne peut être soumise qu'après le PV. Statut actuel : " + mission.getStatut());
        }

        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant de la facture doit être supérieur à 0.");
        }

        if (factureRef == null || factureRef.isBlank()) {
            throw new IllegalArgumentException("La référence de la facture est obligatoire.");
        }

        mission.setMontantFacture(montant);
        mission.setFactureRef(factureRef);
        mission.setStatut(StatutMission.FACTURE_SOUMISE);
        missionRepository.save(mission);

        notificationService.notifier(
                mission.getPrestation().getDossier().getAgentCreateur().getUsername(),
                "Facture soumise — " + mission.getNumeroMission(),
                "Le prestataire " + mission.getPrestataire().getNom()
                        + " a soumis une facture de " + montant + " TND"
                        + " pour le dossier "
                        + mission.getPrestation().getDossier().getNumeroDossier(),
                "FACTURE",
                mission.getPrestation().getDossier()
        );

        historiqueService.enregistrer(
                mission.getPrestation().getDossier(),
                "FACTURE_SOUMISE",
                "Facture soumise : " + montant + " TND — réf : " + factureRef
                        + " — mission " + mission.getNumeroMission(),
                username
        );
    }

    // ═════════ RÉSULTAT MISSION ═════════

    @Transactional
    public void modifierResultatMission(Long missionId,
                                        String commentaire,
                                        MultipartFile[] fichiers) {

        Mission mission = missionRepository.findById(missionId)           // ✅ Cohérent avec @Transactional
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : " + missionId));

        // 1. Récupérer ou créer le ResultatMission lié
        ResultatMission resultat = resultatMissionRepository
                .findByMissionIdWithFichiers(missionId)
                .orElseGet(() -> {
                    ResultatMission r = new ResultatMission();
                    r.setMission(mission);
                    r.setDateCreation(LocalDateTime.now());
                    r.setFichiers(new ArrayList<>());
                    return r;
                });

        // 2. Mettre à jour le commentaire et la date de modification
        resultat.setCommentaire(commentaire);
        resultat.setDateModification(LocalDateTime.now());

        // 3. Mettre à jour pvMission sur la Mission (champ legacy)
        mission.setPvMission(commentaire);

        // 4. Mettre à jour le statut si nécessaire
        if (mission.getStatut() == StatutMission.ASSIGNEE
                || mission.getStatut() == StatutMission.EN_COURS) {
            mission.setStatut(StatutMission.PV_SOUMIS);
        }

        // 5. Sauvegarder les nouveaux fichiers
        if (fichiers != null) {
            for (MultipartFile fichier : fichiers) {
                if (!fichier.isEmpty()) {
                    try {
                        Path dirPath = Paths.get(uploadDir);
                        if (!Files.exists(dirPath)) {
                            Files.createDirectories(dirPath);
                        }

                        String nomServeur = UUID.randomUUID() + "_"
                                + StringUtils.cleanPath(
                                    Objects.requireNonNull(fichier.getOriginalFilename()));

                        Path destination = dirPath.resolve(nomServeur);
                        Files.copy(fichier.getInputStream(), destination,
                                   StandardCopyOption.REPLACE_EXISTING);

                        FichierResultat fr = FichierResultat.builder()
                                .nomFichierOriginal(fichier.getOriginalFilename())
                                .nomFichierServeur(nomServeur)
                                .typeMime(fichier.getContentType())
                                .tailleFichier(fichier.getSize())
                                .dateUpload(LocalDateTime.now())
                                .resultat(resultat)
                                .build();

                        resultat.getFichiers().add(fr);

                    } catch (IOException e) {
                        throw new RuntimeException(
                            "Erreur lors de l'upload du fichier : " + e.getMessage(), e);
                    }
                }
            }
        }

        // 6. Sauvegarder
        resultatMissionRepository.save(resultat);
        missionRepository.save(mission);
    }

    @Transactional
    public void supprimerFichierResultat(Long fichierId, String username) {
        FichierResultat fichier = fichierResultatRepository.findById(fichierId)
                .orElseThrow(() -> new IllegalArgumentException("Fichier introuvable : id=" + fichierId));

        Mission mission = fichier.getResultat().getMission();
        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new SecurityException("Accès refusé");
        }

        try {
            Path path = Paths.get(uploadDir).resolve(fichier.getNomFichierServeur());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Erreur lors de la suppression physique du fichier : " + e.getMessage());
        }

        fichierResultatRepository.delete(fichier);
    }

    // ═════════ UTILITAIRES MISSION ═════════

    @Transactional
    public void changerStatutMission(Long missionId, StatutMission nouveauStatut) {
        Mission mission = getMissionById(missionId);
        mission.setStatut(nouveauStatut);
        missionRepository.save(mission);
    }

    @Transactional                                                         // ✅ Ajouté
    public void updateMission(Long id, String description) {
        Mission m = missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : id=" + id));
        m.setDescription(description);
        missionRepository.save(m);
    }

    @Transactional
    public boolean deletePrestataire(Long id, String username) {           // ✅ Ajouté
        Prestataire prestataire = prestataireRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prestataire introuvable : id=" + id));

        if (!prestataire.getAgentResponsable().getUsername().equals(username)) {
            throw new SecurityException("Accès refusé");
        }

        prestataireRepository.delete(prestataire);
        return true;
    }

    @Transactional(readOnly = true)
    public Mission getMissionByIdWithDetails(Long id) {
        return missionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable : id=" + id));
    }

    // ═════════ UTILITAIRES PRIVÉS ═════════

    private void validerCompatibiliteTypePrestataire(TypePrestation typePrestation,
                                                     Prestataire prestataire) {
        switch (typePrestation) {
            case PROCEDURE_JUDICIAIRE -> {
                if (prestataire.getType() != com.example.contentieux_security.enums.TypePrestataire.AVOCAT) {
                    throw new IllegalStateException(
                            "Une procédure judiciaire requiert un AVOCAT. "
                                    + "Type sélectionné : " + prestataire.getType());
                }
            }
        }
    }

    private String genererNumeroPrestation() {
        String prefix = "PREST-" + LocalDate.now().getYear();
        Optional<String> last = prestationRepository.findLastNumero(prefix);
        int seq = 1;
        if (last.isPresent()) {
            try {
                String[] parts = last.get().split("-");
                seq = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("%s-%05d", prefix, seq);
    }

    private String genererNumeroMission() {
        String prefix = "MISS-" + LocalDate.now().getYear();
        Optional<String> last = missionRepository.findLastNumero(prefix);
        int seq = 1;
        if (last.isPresent()) {
            try {
                String[] parts = last.get().split("-");
                seq = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("%s-%05d", prefix, seq);
    }
}