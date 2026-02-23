package com.example.contentieux_security.controller;

import com.example.contentieux_security.entity.Utilisateur;
import com.example.contentieux_security.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    public List<Utilisateur> getAllUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createUtilisateur(@RequestBody Utilisateur utilisateur) {
        try {
            Utilisateur saved = utilisateurRepository.save(utilisateur);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Erreur interne: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                sb.append(" | Cause: ").append(cause.getClass().getSimpleName()).append(": ")
                        .append(cause.getMessage());
                cause = cause.getCause();
            }
            return ResponseEntity.status(500).body(sb.toString());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> updateUtilisateur(@PathVariable Long id,
            @RequestBody Utilisateur utilisateurDetails) {
        return utilisateurRepository.findById(id)
                .map(utilisateur -> {
                    utilisateur.setNom(utilisateurDetails.getNom());
                    utilisateur.setPrenom(utilisateurDetails.getPrenom());
                    utilisateur.setEmail(utilisateurDetails.getEmail());
                    utilisateur.setTelephone(utilisateurDetails.getTelephone());
                    utilisateur.setRole(utilisateurDetails.getRole());
                    utilisateur.setSpecialite(utilisateurDetails.getSpecialite());
                    if (utilisateurDetails.getPassword() != null && !utilisateurDetails.getPassword().isEmpty()) {
                        utilisateur.setPassword(utilisateurDetails.getPassword());
                    }
                    if (utilisateurDetails.getUsername() != null && !utilisateurDetails.getUsername().isEmpty()) {
                        utilisateur.setUsername(utilisateurDetails.getUsername());
                    }
                    return ResponseEntity.ok(utilisateurRepository.save(utilisateur));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilisateur(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(utilisateur -> {
                    utilisateurRepository.delete(utilisateur);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
