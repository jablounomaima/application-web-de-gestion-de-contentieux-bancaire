package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.DossierContentieux;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResultatMissionService {

    private final MissionRepository         missionRepo;
    private final ResultatMissionRepository resultatRepo;
    private final NotificationService       notificationService;
    private final FileStorageService        fileStorageService;

    /**
     * Soumet le résultat d'une mission par le prestataire.
     * ➡ statut mission → REALISEE
     * ➡ notification agent "Mission avocat terminée"
     */
    public ResultatMission soumettre(Long missionId,
                                     String commentaire,
                                     MultipartFile fichier,
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

        // Construire ou mettre à jour le résultat (re-soumission possible)
        ResultatMission resultat = resultatRepo.findByMissionId(missionId)
                .orElse(new ResultatMission());

        resultat.setMission(mission);
        resultat.setCommentaire(commentaire);
        resultat.setSoumisePar(username);

        // Upload fichier si fourni
        if (fichier != null && !fichier.isEmpty()) {
            // Supprimer l'ancien fichier sur disque si re-soumission
            if (resultat.getNomFichierServeur() != null) {
                fileStorageService.supprimer("missions", resultat.getNomFichierServeur());
            }
            String nomServeur = fileStorageService.stocker(fichier, "missions");
            resultat.setNomFichierOriginal(fichier.getOriginalFilename());
            resultat.setNomFichierServeur(nomServeur);
            resultat.setTypeMime(fichier.getContentType());
            resultat.setTailleFichier(fichier.getSize());
        }

        resultat = resultatRepo.save(resultat);

        // ➡ Statut mission → REALISEE
        mission.setStatut(StatutMission.REALISEE);
        missionRepo.save(mission);

        // ➡ Notification agent — signature exacte : (destinataire, titre, message, type, dossier)
        DossierContentieux dossier = mission.getPrestation().getDossier();
        String nomPrestataire = mission.getPrestataire().getPrenom()
                + " " + mission.getPrestataire().getNom();

        notificationService.notifier(
            dossier.getAgentCreateur().getUsername(),
            "Mission avocat terminee",
            "Le prestataire " + nomPrestataire
                + " a soumis le resultat de la mission "
                + mission.getNumeroMission()
                + ". Commentaire : " + commentaire,
            "PV_MISSION",
            dossier
        );

        log.info("Mission {} marquee REALISEE par {}", mission.getNumeroMission(), username);
        return resultat;
    }

    /**
     * Retourne le résultat soumis pour une mission donnée, ou null si pas encore soumis.
     */
    @Transactional(readOnly = true)
    public ResultatMission getResultat(Long missionId) {
        return resultatRepo.findByMissionId(missionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public ResultatMission getResultatById(Long id) {
        return resultatRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Résultat introuvable : " + id));
    }
}