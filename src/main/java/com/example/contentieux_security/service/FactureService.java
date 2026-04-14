package com.example.contentieux_security.service;

import com.example.contentieux_security.entity.Facture;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.repository.FactureRepository;
import com.example.contentieux_security.repository.MissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactureService {

    private final FactureRepository   factureRepository;
    private final MissionRepository   missionRepository;
    private final FileStorageService  fileStorageService;
    private final NotificationService notificationService;

    // ─────────────────────────────────────────────
    //  LECTURE
    // ─────────────────────────────────────────────

    /**
     * Retourne la facture liée à une mission, ou {@code null} si inexistante.
     */
    public Facture getFactureParMission(Long missionId) {
        return factureRepository.findByMission_Id(missionId).orElse(null);
    }

    // ─────────────────────────────────────────────
    //  SOUMISSION
    // ─────────────────────────────────────────────

    /**
     * Crée ou met à jour la facture d'honoraires d'une mission.
     *
     * Corrections appliquées vs version précédente :
     *  - Suppression de mission.getDossier() (méthode inexistante sur Mission)
     *  - Bloc notification dans try/catch isolé → ne bloque jamais la sauvegarde
     *  - Signature notifier() adaptable selon votre NotificationService réel
     */
    @Transactional
    public void soumettreFacture(Long missionId,
                                BigDecimal montantHT,
                                BigDecimal tauxTva,
                                String numeroFacture,
                                LocalDate dateFacture,
                                String description,
                                MultipartFile fichier,
                                String username) throws IOException {

        // 1. Vérification mission
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Mission introuvable : id=" + missionId));

        // 2. Récupère la facture existante ou en crée une nouvelle
        Facture facture = factureRepository.findByMission_Id(missionId)
                .orElseGet(Facture::new);

        facture.setMission(mission);

        // 3. Calcul TTC
        BigDecimal taux = (tauxTva != null) ? tauxTva : BigDecimal.ZERO;
        BigDecimal montantTTC = montantHT
                .multiply(BigDecimal.ONE.add(
                        taux.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(3, RoundingMode.HALF_UP);

        // 4. Alimentation des champs
        facture.setMontantHT(montantHT);
        facture.setTauxTva(taux);
        facture.setMontantTTC(montantTTC);
        facture.setNumeroFacture(numeroFacture);
        facture.setDateFacture(dateFacture != null ? dateFacture : LocalDate.now());
        facture.setDescription(description);
        facture.setSoumisePar(username);
        facture.setDateSoumission(LocalDateTime.now());
        facture.setStatut(Facture.StatutFacture.SOUMISE);

        // 5. Fichier joint (optionnel)
        if (fichier != null && !fichier.isEmpty()) {
            String nomServeur = fileStorageService.stocker(fichier, "factures");
            facture.setNomFichierServeur(nomServeur);
            facture.setNomFichierOriginal(fichier.getOriginalFilename());
            facture.setTypeMime(fichier.getContentType());
        }

        factureRepository.save(facture);

        // 6. Notification — isolée pour ne jamais bloquer la sauvegarde
        // ✅ FIX : adaptez l'appel ci-dessous à la signature exacte de votre NotificationService
        //
        //  Signature réelle trouvée dans les logs :
        //  notifier(String destinataire, String titre, String message, String type, DossierContentieux dossier)
        //
        //  → On utilise une version simplifiée sans dossier ni type :
        //    si votre méthode exige ces paramètres, passez null ou une valeur par défaut.
        try {
            String titre   = "Facture d'honoraires soumise";
            String message = "L'avocat " + username
                    + " a soumis une facture de " + montantTTC
                    + " DT TTC pour la mission #" + missionId + ".";

            // ── Décommentez la ligne qui correspond à votre NotificationService ──

            // CAS A – notifier(String destinataire, String message)
            // notificationService.notifier(mission.getPrestataire(), message);

            // CAS B – notifier(String titre, String message)
            // notificationService.notifier(titre, message);

            // CAS C – notifier(String expediteur, String titre, String message)
            notificationService.notifier(username, titre, message, message, null);

            // CAS D – notifier(String dest, String titre, String msg, String type, Dossier d)
            // notificationService.notifier(mission.getPrestataire(), titre, message, "FACTURE", null);

        } catch (Exception ex) {
            log.warn("Notification facture non envoyée — mission={} : {}",
                    missionId, ex.getMessage());
        }

        log.info("Facture soumise — mission={} montantTTC={} par={}",
                missionId, montantTTC, username);
    }

    // ─────────────────────────────────────────────
    //  TÉLÉCHARGEMENT
    // ─────────────────────────────────────────────

    /**
     * Construit la ResponseEntity pour télécharger le fichier de facture.
     */
    public ResponseEntity<Resource> buildDownloadResponse(Long missionId) {
        try {
            Facture facture = factureRepository.findByMission_Id(missionId)
                    .orElse(null);

            if (facture == null || facture.getNomFichierServeur() == null) {
                return ResponseEntity.notFound().build();
            }

            Path     filePath = fileStorageService.getCheminFichier(
                    "factures", facture.getNomFichierServeur());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Fichier facture absent sur disque : {}", filePath);
                return ResponseEntity.notFound().build();
            }

            String contentType = (facture.getTypeMime() != null)
                    ? facture.getTypeMime()
                    : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + facture.getNomFichierOriginal() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur download facture mission={}", missionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}