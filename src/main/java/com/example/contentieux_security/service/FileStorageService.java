package com.example.contentieux_security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    // Dans application.properties : app.upload.dir=uploads
    // Sera créé à côté du jar / dans le projet
    @Value("${app.upload.dir:uploads}")
    private String uploadBaseDir;

    @PostConstruct
    public void init() {
        try {
            // Crée les dossiers au démarrage s'ils n'existent pas
            Files.createDirectories(Paths.get(uploadBaseDir, "affaires"));
            Files.createDirectories(Paths.get(uploadBaseDir, "missions"));
            log.info("Dossiers d'upload initialisés : {}", Paths.get(uploadBaseDir).toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer les dossiers d'upload", e);
        }
    }

    /**
     * Stocke un fichier uploadé et retourne le nom unique généré.
     *
     * @param file       le fichier MultipartFile reçu
     * @param sousRepertoire  ex: "affaires" ou "missions"
     * @return           nom du fichier sur le serveur (UUID_nomoriginal.ext)
     */
    public String stocker(MultipartFile file, String sousRepertoire) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide ou absent");
        }

        String nomOriginal = file.getOriginalFilename();
        if (nomOriginal == null) nomOriginal = "fichier";

        // Sécuriser le nom (éviter traversée de répertoire)
        nomOriginal = nomOriginal.replaceAll("[^a-zA-Z0-9._\\-]", "_");

        // Nom unique sur le serveur : UUID + nom original
        String nomServeur = UUID.randomUUID().toString().substring(0, 8) + "_" + nomOriginal;

        Path destination = Paths.get(uploadBaseDir, sousRepertoire, nomServeur);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        log.info("Fichier stocké : {}", destination.toAbsolutePath());
        return nomServeur;
    }

    /**
     * Retourne le chemin complet d'un fichier stocké.
     */
    public Path getCheminFichier(String sousRepertoire, String nomServeur) {
        return Paths.get(uploadBaseDir, sousRepertoire, nomServeur);
    }

    /**
     * Supprime un fichier du disque.
     */
    public void supprimer(String sousRepertoire, String nomServeur) {
        try {
            Path fichier = Paths.get(uploadBaseDir, sousRepertoire, nomServeur);
            Files.deleteIfExists(fichier);
            log.info("Fichier supprimé : {}", fichier);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}/{}", sousRepertoire, nomServeur);
        }
    }

    /**
     * Chemin relatif pour l'URL de téléchargement.
     */
    public String getCheminRelatif(String sousRepertoire, String nomServeur) {
        return sousRepertoire + "/" + nomServeur;
    }
}