package com.example.contentieux_security.controller;

import com.example.contentieux_security.service.NotificationService;
import com.example.contentieux_security.entity.*;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.enums.StatutPrestation;
import com.example.contentieux_security.enums.TypePrestation;
import com.example.contentieux_security.repository.*;
import com.example.contentieux_security.service.MissionService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agent/dossiers/{dossierId}/missions")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
@Slf4j
public class DossierMissionController {

    private final DossierRepository        dossierRepository;
    private final MissionRepository        missionRepository;
    private final PrestationRepository     prestationRepository;
    private final PrestataireRepository    prestataireRepository;
    private final MissionService           missionService;
    private final NotificationService      notificationService;
    private final AgentBancaireRepository  agentBancaireRepository;

    // ══════════════════════════════════════════════════════════════
    // MAPPERS DTO
    // ══════════════════════════════════════════════════════════════

    private Map<String, Object> mapPrestataire(Prestataire p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id",         p.getId());
        dto.put("username",   p.getUsername());
        dto.put("nom",        p.getNom());
        dto.put("prenom",     p.getPrenom());
        dto.put("email",      p.getEmail());
        dto.put("telephone",  p.getTelephone());
        dto.put("specialite", p.getSpecialite());
        dto.put("actif",      p.isActif());
        dto.put("type",       p.getType() != null ? p.getType().name() : null);
        return dto;
    }

    private Map<String, Object> mapPrestation(Prestation p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id",               p.getId());
        dto.put("numeroPrestation", p.getNumeroPrestation());
        dto.put("statut",           p.getStatut() != null ? p.getStatut().name() : null);
        dto.put("type",             p.getType()   != null ? p.getType().name()   : null);
        dto.put("description",      p.getDescription());
        dto.put("dateCreation",     p.getDateCreation());
        return dto;
    }

    // ══════════════════════════════════════════════════════════════
    // GET — liste missions + données formulaire
    // ══════════════════════════════════════════════════════════════

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listeMissions(@PathVariable Long dossierId, Authentication auth) {
        try {
            DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId).orElse(null);
            if (dossier == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Dossier introuvable"));
            if (!auth.getName().equals(dossier.getCreePar()))
                return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
    
            List<Mission> missions = missionRepository.findByDossierIdWithPrestataire(dossierId);
            AgentBancaire agent = agentBancaireRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Agent introuvable"));
            List<Prestataire> prestataires = prestataireRepository.findByAgence_Id(agent.getAgence().getId());
            List<Prestation>  prestations  = prestationRepository.findByDossier_Id(dossierId);
    
            // ── Dossier ──────────────────────────────────────────────
            Map<String, Object> dossierMap = new HashMap<>();
            dossierMap.put("id",            dossier.getId());
            dossierMap.put("numeroDossier", dossier.getNumeroDossier());
            dossierMap.put("libelle",       dossier.getLibelle());
            dossierMap.put("statut",        dossier.getStatut() != null ? dossier.getStatut().name() : null);
            dossierMap.put("montant",       dossier.calculerSolde());
            if (dossier.getClient() != null) {
                Map<String, Object> clientMap = new HashMap<>();
                clientMap.put("nom",    dossier.getClient().getNom());
                clientMap.put("prenom", dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "");
                clientMap.put("email",  dossier.getClient().getEmail() != null ? dossier.getClient().getEmail() : "");
                dossierMap.put("client", clientMap);
            }
    
            // ── Missions ─────────────────────────────────────────────
            List<Map<String, Object>> missionsMap = new ArrayList<>();
            for (Mission m : missions) {
                Map<String, Object> mm = new HashMap<>();
                mm.put("id",              m.getId());
                mm.put("numeroMission",   m.getNumeroMission());
                mm.put("statut",          m.getStatut() != null ? m.getStatut().name() : null);
                mm.put("description",     m.getDescription());
                mm.put("dateAssignation", m.getDateAssignation());
                mm.put("dateFinPrevue",   m.getDateFinPrevue());
                if (m.getPrestataire() != null) {
                    Map<String, Object> pm = new HashMap<>();
                    pm.put("id",       m.getPrestataire().getId());
                    pm.put("nom",      m.getPrestataire().getNom());
                    pm.put("prenom",   m.getPrestataire().getPrenom());
                    pm.put("username", m.getPrestataire().getUsername());
                    pm.put("type",     m.getPrestataire().getType() != null ? m.getPrestataire().getType().name() : "");
                    mm.put("prestataire", pm);
                }
                if (m.getPrestation() != null) {
                    mm.put("typePrestation", m.getPrestation().getType() != null ? m.getPrestation().getType().name() : null);
                }
                missionsMap.add(mm);
            }
    
            // ── Prestataires ─────────────────────────────────────────
            List<Map<String, Object>> prestatairesMap = prestataires.stream().map(p -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id",         p.getId());
                pm.put("username",   p.getUsername());
                pm.put("nom",        p.getNom());
                pm.put("prenom",     p.getPrenom());
                pm.put("email",      p.getEmail() != null ? p.getEmail() : "");
                pm.put("telephone",  p.getTelephone() != null ? p.getTelephone() : "");
                pm.put("specialite", p.getSpecialite() != null ? p.getSpecialite() : "");
                pm.put("actif",      p.isActif());
                pm.put("type",       p.getType() != null ? p.getType().name() : null);
                return pm;
            }).collect(Collectors.toList());
    
            // ── Prestations ──────────────────────────────────────────
            List<Map<String, Object>> prestationsMap = prestations.stream().map(p -> {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id",               p.getId());
                pm.put("numeroPrestation", p.getNumeroPrestation());
                pm.put("statut",           p.getStatut() != null ? p.getStatut().name() : null);
                pm.put("type",             p.getType()   != null ? p.getType().name()   : null);
                pm.put("description",      p.getDescription());
                pm.put("dateCreation",     p.getDateCreation());
                return pm;
            }).collect(Collectors.toList());
    
            String clientNom = dossier.getClient() != null
                    ? dossier.getClient().getNom() + " " +
                      (dossier.getClient().getPrenom() != null ? dossier.getClient().getPrenom() : "")
                    : "—";
    
            // ── Réponse ──────────────────────────────────────────────
            Map<String, Object> response = new HashMap<>();
            response.put("nbAssignee",       missions.stream().filter(m -> m.getStatut() == StatutMission.ASSIGNEE).count());
            response.put("nbEnCours",        missions.stream().filter(m -> m.getStatut() == StatutMission.EN_COURS).count());
            response.put("nbPvSoumis",       missions.stream().filter(m -> m.getStatut() == StatutMission.PV_SOUMIS).count());
            response.put("nbFactureSoumise", missions.stream().filter(m -> m.getStatut() == StatutMission.FACTURE_SOUMISE).count());
            response.put("nbTerminee",       missions.stream().filter(m -> m.getStatut() == StatutMission.TERMINEE).count());
            response.put("nbRetard",         missions.stream()
                    .filter(m -> m.getDateFinPrevue() != null
                              && m.getDateFinPrevue().isBefore(LocalDate.now())
                              && m.getStatut() != StatutMission.TERMINEE).count());
            response.put("dossier",      dossierMap);
            response.put("clientNom",    clientNom);
            response.put("missions",     missionsMap);
            response.put("prestataires", prestatairesMap);
            response.put("prestations",  prestationsMap);
    
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            log.error("Erreur listeMissions dossierId={} : {}", dossierId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"));
        }
    }
    // ══════════════════════════════════════════════════════════════
    // POST — créer mission
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/creer")
    public ResponseEntity<?> creerMission(@PathVariable Long dossierId,
                                          @RequestBody Map<String, Object> body,
                                          Authentication auth) {
        try {
            // ── 1. Extraction ─────────────────────────────────────────
            String prestataireIdStr = body.get("prestataireId") != null ? body.get("prestataireId").toString() : null;
            String typePrestation   = body.get("typePrestation") != null ? body.get("typePrestation").toString() : null;
            String description      = body.get("description") != null ? body.get("description").toString() : null;
            String dateFinStr       = body.get("dateFinPrevue") != null ? body.get("dateFinPrevue").toString() : null;
    
            // ── 2. Validation ─────────────────────────────────────────
            if (prestataireIdStr == null || prestataireIdStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "prestataireId est requis"));
            }
            if (typePrestation == null || typePrestation.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "typePrestation est requis"));
            }
            if (description == null || description.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "description est requise"));
            }
    
            // ── 3. Conversion ─────────────────────────────────────────
            Long prestataireId = Long.valueOf(prestataireIdStr.trim());
            TypePrestation enumType = TypePrestation.valueOf(typePrestation.trim());
    
            LocalDate dateFinPrevue = null;
            if (dateFinStr != null && !dateFinStr.isBlank()) {
                dateFinPrevue = LocalDate.parse(dateFinStr.trim());
            }
    
            // ── 4. Dossier ────────────────────────────────────────────
            DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));
    
            if (auth == null || !auth.getName().equals(dossier.getCreePar())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }
    
            // ── 5. Agent ──────────────────────────────────────────────
            AgentBancaire agent = agentBancaireRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Agent introuvable"));
    
            // ── 6. Prestation ─────────────────────────────────────────
            Prestation prestation = prestationRepository.findByDossier_Id(dossierId)
                    .stream()
                    .filter(p -> p.getType() == enumType)
                    .findFirst()
                    .orElseGet(() -> {
                        long nb = prestationRepository.count() + 1;
    
                        Prestation p = Prestation.builder()
                                .dossier(dossier)
                                .type(enumType)
                                .statut(StatutPrestation.EN_COURS)
                                .dateCreation(LocalDateTime.now())
                                .agentCreateur(agent)
                                .numeroPrestation(String.format("PREST-%d-%05d", LocalDate.now().getYear(), nb))
                                .build();
    
                        return prestationRepository.save(p);
                    });
    
            // ── 7. Prestataire ────────────────────────────────────────
            Prestataire prestataire = prestataireRepository.findById(prestataireId)
                    .orElseThrow(() -> new RuntimeException("Prestataire introuvable"));
    
            // ── 8. Mission ────────────────────────────────────────────
            Mission mission = new Mission();
            mission.setPrestation(prestation);
            mission.setPrestataire(prestataire);
            mission.setDescription(description.trim());
            mission.setDateFinPrevue(dateFinPrevue);
            mission.setDateAssignation(LocalDate.now());
            mission.setStatut(StatutMission.ASSIGNEE);
    
            long count = missionRepository.count() + 1;
            mission.setNumeroMission(String.format("MISS-%d-%05d", LocalDate.now().getYear(), count));
    
            mission = missionRepository.save(mission);
    
            // ── 9. 🔥 Notification complète ───────────────────────────
            String clientNom = "—";
            String email = "—";
            String tel = "—";
    
            if (dossier.getClient() != null) {
                clientNom = (dossier.getClient().getNom() + " " +
                             dossier.getClient().getPrenom());
    
                email = dossier.getClient().getEmail() != null ? dossier.getClient().getEmail() : "—";
                tel   = dossier.getClient().getTelephone() != null ? dossier.getClient().getTelephone() : "—";
            }
    
            String echeance = dateFinPrevue != null
                    ? dateFinPrevue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "Non définie";
    
            String message = String.format(
                    "MISSION : %s\n" +
                    "Dossier : %s\n" +
                    "Client  : %s\n" +
                    "Email   : %s\n" +
                    "Tel     : %s\n" +
                    "Type    : %s\n" +
                    "Desc    : %s\n" +
                    "Échéance: %s",
    
                    mission.getNumeroMission(),
                    dossier.getNumeroDossier(),
                    clientNom,
                    email,
                    tel,
                    prestation.getType(),
                    description,
                    echeance
            );
    
            notificationService.notifier(
                    prestataire.getUsername(),
                    "Nouvelle mission",
                    message,
                    "MISSION",
                    dossier
            );
    
            // ── 10. Réponse ───────────────────────────────────────────
            return ResponseEntity.ok(Map.of(
                    "message", "Mission créée",
                    "mission", mission.getNumeroMission()
            ));
    
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ══════════════════════════════════════════════════════════════
    // PUT — changer statut
    // ══════════════════════════════════════════════════════════════

    @PutMapping("/{missionId}/statut")
    public ResponseEntity<?> changerStatut(@PathVariable Long dossierId,
                                           @PathVariable Long missionId,
                                           @RequestBody Map<String, String> body) {
        try {
            StatutMission statut = StatutMission.valueOf(body.get("statut"));
            missionService.changerStatut(missionId, statut);
            return ResponseEntity.ok(Map.of("message", "Statut mis à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE — supprimer mission
    // ══════════════════════════════════════════════════════════════

    @DeleteMapping("/{missionId}")
    public ResponseEntity<?> supprimer(@PathVariable Long dossierId,
                                       @PathVariable Long missionId,
                                       Authentication auth) {
        try {
            missionService.supprimerMission(missionId, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Mission supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}