package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.FichierResultat;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.repository.ResultatMissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResultatMissionService {

    private final MissionRepository missionRepo;
    private final ResultatMissionRepository resultatRepo;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final FichierResultatService fichierResultatService;

    /**
     * Soumettre un résultat avec plusieurs fichiers
     */
    public ResultatMission soumettre(Long missionId,
                                     String commentaire,
                                     List<MultipartFile> fichiers,
                                     String username) throws IOException {

        Mission mission = missionRepo.findById(missionId)
            .orElseThrow(() -> new RuntimeException("Mission introuvable : " + missionId));

        // Vérifier que c'est bien son prestataire
        if (!mission.getPrestataire().getUsername().equals(username)) {
            throw new RuntimeException("Accès non autorisé à cette mission");
        }

        // Vérifier que la mission n'est pas déjà clôturée
        if (mission.getStatut() == StatutMission.REALISEE
                || mission.getStatut() == StatutMission.TERMINEE) {
            throw new RuntimeException("Cette mission est déjà clôturée");
        }

        // Créer un NOUVEAU résultat (historique conservé)
        ResultatMission resultat = new ResultatMission();
        resultat.setMission(mission);
        resultat.setCommentaire(commentaire);
        resultat.setSoumisePar(username);
        resultat.setDateSoumission(LocalDateTime.now());
        resultat.setFichiers(new ArrayList<>());

        // Sauvegarder d'abord pour obtenir l'ID
        resultat = resultatRepo.save(resultat);

        // Traiter les fichiers multiples
        int fichiersUploades = 0;
        if (fichiers != null) {
            for (MultipartFile fichier : fichiers) {
                if (fichier != null && !fichier.isEmpty()) {
                    FichierResultat fr = creerFichierResultat(fichier, resultat);
                    resultat.addFichier(fr);
                    fichiersUploades++;
                }
            }
        }

        // Sauvegarder les fichiers
        if (!resultat.getFichiers().isEmpty()) {
            fichierResultatService.saveAll(resultat.getFichiers());
        }

        // ➡ Statut mission → REALISEE
        mission.setStatut(StatutMission.REALISEE);
        missionRepo.save(mission);

        // ➡ Notification agent
        DossierContentieux dossier = mission.getPrestation().getDossier();
        String nomPrestataire = mission.getPrestataire().getPrenom()
                + " " + mission.getPrestataire().getNom();

        String messageNotif = "Le prestataire " + nomPrestataire
                + " a soumis un résultat pour la mission "
                + mission.getNumeroMission();
        
        if (commentaire != null && !commentaire.isEmpty()) {
            messageNotif += ". Commentaire : " + commentaire;
        }
        
        if (fichiersUploades > 0) {
            messageNotif += " (" + fichiersUploades + " fichier(s) joint(s))";
        }

        notificationService.notifier(
            dossier.getAgentCreateur().getUsername(),
            "Mission avocat terminée",
            messageNotif,
            "PV_MISSION",
            dossier
        );

        log.info("Mission {} marquée REALISEE par {} avec {} fichier(s)", 
                mission.getNumeroMission(), username, fichiersUploades);
        
        return resultat;
    }

    private FichierResultat creerFichierResultat(MultipartFile fichier, ResultatMission resultat) throws IOException {
        String nomServeur = fileStorageService.stocker(fichier, "missions");
        
        FichierResultat fr = new FichierResultat();
        fr.setNomFichierOriginal(fichier.getOriginalFilename());
        fr.setNomFichierServeur(nomServeur);
        fr.setTypeMime(fichier.getContentType());
        fr.setTailleFichier(fichier.getSize());
        fr.setDateUpload(LocalDateTime.now());
        fr.setResultat(resultat);
        
        return fr;
    }

    // ← AJOUTER CETTE MÉTHODE (pour compatibilité ancien code)
    @Transactional(readOnly = true)
    public ResultatMission getResultat(Long missionId) {
        return resultatRepo.findByMission_Id(missionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ResultatMission> findByMissionOrderByDateSoumissionDesc(Mission mission) {
        // ✅ FIX : missionId n'existe pas ici → utiliser mission.getId()
        return resultatRepo.findByMission_IdOrderByDateSoumissionDesc(mission.getId());
    }

    @Transactional(readOnly = true)
    public ResultatMission getDernierResultat(Long missionId) {
        return resultatRepo.findTopByMissionIdOrderByDateSoumissionDesc(missionId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ResultatMission getResultatById(Long id) {
        return resultatRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Résultat introuvable : " + id));
    }

    // Méthodes utilitaires
    private String formaterTaille(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / 1048576.0);
    }

    private String getIconeType(String mimeType) {
        if (mimeType == null) return "bi-file-earmark";
        if (mimeType.equals("application/pdf")) return "bi-file-earmark-pdf text-danger";
        if (mimeType.startsWith("image/")) return "bi-file-earmark-image text-info";
        if (mimeType.contains("word") || mimeType.contains("document")) return "bi-file-earmark-word text-primary";
        if (mimeType.contains("sheet") || mimeType.contains("excel")) return "bi-file-earmark-excel text-success";
        if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) return "bi-file-earmark-ppt text-warning";
        if (mimeType.contains("text")) return "bi-file-earmark-text text-secondary";
        return "bi-file-earmark-check";
    }

    public List<ResultatMission> getResultatsByMission(Long missionId) {
        return resultatRepo.findByMission_IdOrderByDateSoumissionDesc(missionId);
    }
}