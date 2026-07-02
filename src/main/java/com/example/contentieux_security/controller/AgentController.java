package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.AgentProfileUpdateRequest;
import com.example.contentieux_security.dto.GarantieAjoutRequest;
import com.example.contentieux_security.dto.PasswordChangeRequest;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Mission;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.enums.DossierStatus;
import com.example.contentieux_security.enums.StatutMission;
import com.example.contentieux_security.repository.DossierRepository;
import com.example.contentieux_security.repository.MissionRepository;
import com.example.contentieux_security.service.AgentBancaireService;
import com.example.contentieux_security.service.ClientService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.MissionService;
import com.example.contentieux_security.service.PrestataireService;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.contentieux_security.entity.FichierResultat;
import com.example.contentieux_security.entity.ResultatMission;
import com.example.contentieux_security.repository.ResultatMissionRepository;
import lombok.extern.slf4j.Slf4j;
import com.example.contentieux_security.service.NotificationService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentBancaireService agentService;
    private final PrestataireService prestataireService;
    private final ClientService clientService;
    private final DossierService dossierService;
    private final DossierRepository dossierRepository;
    private final MissionService missionService;
    private final MissionRepository missionRepository;
    private final ResultatMissionRepository resultatMissionRepository;
    private final NotificationService notificationService;

    // ══════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public ResponseEntity<?> agentDashboard(Principal principal) {
        AgentBancaire agent = agentService.findAgentByUsername(principal.getName());
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Compte non enregistré en base."));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("agent", agent);
        response.put("agence", agent.getAgence());
        response.put("username", principal.getName());
        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════════════════
    //  CLIENTS
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/clients")
    public ResponseEntity<?> gererClients(Principal principal) {
        AgentBancaire agent = agentService.findAgentByUsername(principal.getName());
        if (agent == null || agent.getAgence() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Agent ou agence non trouvé"));
        }
        List<Client> clients = clientService.findByAgence(agent.getAgence());
        return ResponseEntity.ok(clients);
    }

    @PostMapping("/clients/creer")
    public ResponseEntity<?> creerClient(@RequestBody Client nouveauClient, Principal principal) {
        try {
            AgentBancaire agent = agentService.findAgentByUsername(principal.getName());
            if (agent == null || agent.getAgence() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Agent ou agence non trouvé"));
            }
            nouveauClient.setAgence(agent.getAgence());
            nouveauClient.setDateInscription(LocalDate.now());
            Client saved = clientService.save(nouveauClient);
            return ResponseEntity.ok(Map.of("message", "Client " + saved.getTypeClient() + " créé avec succès !", "client", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<?> getClient(@PathVariable Long id, Principal principal) {
        Client client = clientService.findById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    @PutMapping("/clients/{id}")
    @Transactional
    public ResponseEntity<?> updateClient(@PathVariable Long id, @RequestBody Client client, Principal principal) {
        try {
            Client existing = clientService.findById(id);
            if (existing == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Client non trouvé"));
            }
            if (client.getNom() != null)           existing.setNom(client.getNom());
            if (client.getPrenom() != null)        existing.setPrenom(client.getPrenom());
            if (client.getTypeClient() != null)    existing.setTypeClient(client.getTypeClient());
            if (client.getCin() != null)           existing.setCin(client.getCin());
            if (client.getRaisonSociale() != null) existing.setRaisonSociale(client.getRaisonSociale());
            if (client.getRne() != null)           existing.setRne(client.getRne());
            if (client.getEmail() != null)         existing.setEmail(client.getEmail());
            if (client.getTelephone() != null)     existing.setTelephone(client.getTelephone());
            if (client.getAdresse() != null)       existing.setAdresse(client.getAdresse());

            Client updated = clientService.save(existing);

            Map<String, Object> clientResponse = new HashMap<>();
            clientResponse.put("id",            updated.getId());
            clientResponse.put("nom",           updated.getNom());
            clientResponse.put("prenom",        updated.getPrenom());
            clientResponse.put("typeClient",    updated.getTypeClient());
            clientResponse.put("cin",           updated.getCin());
            clientResponse.put("raisonSociale", updated.getRaisonSociale());
            clientResponse.put("rne",           updated.getRne());
            clientResponse.put("email",         updated.getEmail());
            clientResponse.put("telephone",     updated.getTelephone());
            clientResponse.put("adresse",       updated.getAdresse());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Client mis à jour avec succès !");
            response.put("client",  clientResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/clients/{id}/dossiers")
    public ResponseEntity<?> voirDossiersClient(@PathVariable Long id, Principal principal) {
        try {
            AgentBancaire agent = agentService.findAgentByUsername(principal.getName());
            if (agent == null || agent.getAgence() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Agent ou agence non trouvé"));
            }
            Client client = clientService.findById(id);
            if (client == null || client.getAgence() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Client non trouvé ou sans agence"));
            }
            if (!client.getAgence().getId().equals(agent.getAgence().getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Client non autorisé pour cette agence"));
            }
            List<DossierContentieux> dossiers = dossierService.findByClientId(id);
            Map<String, Object> response = new HashMap<>();
            response.put("client", client);
            response.put("dossiers", dossiers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MOT DE PASSE & PROFIL
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {
        try {
            agentService.changePassword(getCurrentUsername(), request);
            return ResponseEntity.ok(Map.of("message", "Mot de passe changé !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        AgentBancaire agent = agentService.findAgentByUsername(getCurrentUsername());
        if (agent == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Agent non trouvé"));
        }
        return ResponseEntity.ok(agent);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody AgentProfileUpdateRequest request) {
        try {
            agentService.updateProfile(getCurrentUsername(), request);
            return ResponseEntity.ok(Map.of("message", "Profil mis à jour !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PRESTATAIRES
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/prestataires")
    public ResponseEntity<?> listPrestataires(Principal principal) {
        List<Prestataire> prestataires = prestataireService.getPrestatairesParAgent(principal.getName());
        return ResponseEntity.ok(prestataires);
    }

    @PostMapping("/prestataires")
    public ResponseEntity<?> creerPrestataire(@RequestBody PrestataireCreationRequest request, Principal principal) {
        try {
            Prestataire p = prestataireService.creerPrestataire(request, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Prestataire créé !", "prestataire", p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/prestataires/{id}")
    public ResponseEntity<?> getPrestataire(@PathVariable("id") Long id) {
        Prestataire prestataire = prestataireService.findById(id);
        if (prestataire == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(prestataire);
    }

    @PutMapping("/prestataires/{id}")
    public ResponseEntity<?> updatePrestataire(@PathVariable("id") Long id,
                                               @RequestBody PrestataireCreationRequest request,
                                               Principal principal) {
        try {
            prestataireService.updatePrestataire(id, request, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Prestataire mis à jour avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/prestataires/{id}")
    public ResponseEntity<?> deletePrestataire(@PathVariable("id") Long id, Principal principal) {
        try {
            boolean deleted = prestataireService.supprimerPrestataire(id, principal.getName());
            if (deleted) return ResponseEntity.ok(Map.of("message", "Prestataire supprimé avec succès !"));
            return ResponseEntity.badRequest().body(Map.of("error", "Impossible de supprimer ce prestataire."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/prestataires/{id}/toggle")
    public ResponseEntity<?> togglePrestataire(@PathVariable("id") Long id, Principal principal) {
        try {
            Prestataire updated = prestataireService.toggleActif(id, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Statut modifié", "actif", updated.isActif()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DOSSIERS / MISSIONS
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/dossiers/risques/{risqueId}/garanties")
    public ResponseEntity<?> ajouterGarantie(@PathVariable Long risqueId,
                                             @RequestBody GarantieAjoutRequest request,
                                             Authentication authentication) {
        try {
            dossierService.ajouterGarantie(risqueId, request, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Garantie ajoutée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/dossiers/{id}/ressoumettre")
    
    public ResponseEntity<?> ressoumettreDossier(@PathVariable Long id, Principal principal) {
        try {
            dossierService.ressoumettre(id, principal.getName());
            return ResponseEntity.ok(Map.of("message", "Dossier ressoumis aux validateurs ✅"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/missions/{id}/valider")
    @Transactional
    public ResponseEntity<?> validerMission(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Principal principal) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable : " + id));

            String prestataireUsername = mission.getPrestataire() != null
                    ? mission.getPrestataire().getUsername() : null;
            String numeroMission = mission.getNumeroMission();

            missionService.validerMission(id, body.get("commentaire"), principal.getName());

            if (prestataireUsername != null) {
                notificationService.notifierSansDossier(
                    prestataireUsername,
                    "🏁 Mission clôturée",
                    "La mission " + numeroMission + " a été validée et clôturée par l'agent bancaire.",
                    "MISSION_CLOTUREE",
                    "/prestataire/missions/" + id
                );
                log.info("📨 Prestataire {} notifié — mission {} clôturée", prestataireUsername, numeroMission);
            }

            return ResponseEntity.ok(Map.of("message", "Mission validée avec succès ✔"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/missions/{id}/rejeter")
    @Transactional
    public ResponseEntity<?> rejeterMission(@PathVariable("id") Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication authentication) {
        try {
            Mission mission = missionRepository.findByIdWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Mission introuvable"));

            String prestataireUsername = mission.getPrestataire() != null
                    ? mission.getPrestataire().getUsername() : null;
            String numeroMission = mission.getNumeroMission();
            String commentaire   = body.get("commentaire");

            mission.setStatut(StatutMission.REJETEE);
            mission.setDateValidationAgent(LocalDateTime.now());
            mission.setValideParAgent(authentication.getName());
            mission.setCommentaireAgent(commentaire);
            mission.setPvValide(false);
            mission.setFactureValide(false);
            missionRepository.save(mission);

            if (prestataireUsername != null) {
                notificationService.notifierSansDossier(
                    prestataireUsername,
                    "❌ Mission rejetée — resoumettre requis",
                    "La mission " + numeroMission
                    + " a été rejetée par l'agent bancaire."
                    + (commentaire != null && !commentaire.isBlank() ? " Motif : " + commentaire : "")
                    + " Merci de resoumettre votre PV et votre facture.",
                    "MISSION_REJETEE",
                    "/prestataire/missions/" + id
                );
                log.info("📨 Prestataire {} notifié — mission {} rejetée", prestataireUsername, numeroMission);
            }

            return ResponseEntity.ok(Map.of("message", "Mission rejetée"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }   // ← accolade fermante de rejeterMission (manquait dans l'original)

    // ══════════════════════════════════════════════════════════════
    //  PROFIL COURANT
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/mon-profil")
    public ResponseEntity<?> getMonProfil(Principal principal) {
        try {
            AgentBancaire agent = agentService.findByUsername(principal.getName());
            return ResponseEntity.ok(Map.of(
                "username", agent.getUsername(),
                "nom",      agent.getNom(),
                "prenom",   agent.getPrenom(),
                "actif",    agent.isActif()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RÉSULTATS PRESTATAIRES PAR DOSSIER
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/dossiers/{dossierId}/resultats-prestataires")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getResultatsPrestataires(@PathVariable Long dossierId,
                                                      Authentication auth) {
        try {
            DossierContentieux dossier = dossierRepository.findByIdWithDetails(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier introuvable"));

            if (!dossier.getCreePar().equals(auth.getName())) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès refusé"));
            }

            List<Mission> missions = missionRepository.findByDossierIdWithFullDetails(dossierId);
            List<Map<String, Object>> resultats = new ArrayList<>();

            for (Mission m : missions) {
                Map<String, Object> item = new HashMap<>();
                item.put("missionId",       m.getId());
                item.put("numeroMission",   m.getNumeroMission());
                item.put("statut",          m.getStatut() != null ? m.getStatut().name() : null);
                item.put("statutLibelle",   m.getStatut() != null ? m.getStatut().getLibelle() : null);
                item.put("description",     m.getDescription());
                item.put("dateAssignation", m.getDateAssignation());
                item.put("dateFinPrevue",   m.getDateFinPrevue());
                item.put("typePrestation",  m.getPrestation() != null ? m.getPrestation().getType() : null);

                if (m.getPrestataire() != null) {
                    var p = m.getPrestataire();
                    item.put("prestataire", Map.of(
                        "id",         p.getId(),
                        "nom",        p.getNom(),
                        "prenom",     p.getPrenom(),
                        "username",   p.getUsername(),
                        "specialite", p.getSpecialite() != null ? p.getSpecialite() : "",
                        "type",       p.getType() != null ? p.getType().name() : ""
                    ));
                }

                item.put("pvTexte",          m.getPvMission());
                item.put("dateValidationPv", m.getDateValidationPv());
                item.put("factureRef",            m.getFactureRef());
                item.put("montantFacture",        m.getMontantFacture());
                item.put("dateValidationFacture", m.getDateValidationFacture());
                item.put("factureValide",         m.getFactureValide());
                item.put("valideParValidateur",   m.getValideParAgent());
                item.put("commentaireValidateur", m.getCommentaireAgent());

                boolean factureValideeFinancier =
                    Boolean.TRUE.equals(m.getFactureValide())
                    && m.getStatut() == StatutMission.FACTURE_VALIDEE;

                boolean factureRejeteeFinancier =
                    Boolean.FALSE.equals(m.getFactureValide())
                    && m.getStatut() == StatutMission.FACTURE_REJETEE;

                item.put("factureValideeParFinancier", factureValideeFinancier);
                item.put("factureRejeteeParFinancier", factureRejeteeFinancier);
                item.put("peutEtreValideeParAgent",    factureValideeFinancier);
                item.put("commentaireAgent",           m.getCommentaireAgent());

                List<Map<String, Object>> fichiersList = new ArrayList<>();
                resultatMissionRepository.findByMissionIdWithFichiers(m.getId()).ifPresent(r -> {
                    if (r.getFichiers() != null) {
                        for (FichierResultat f : r.getFichiers()) {
                            Map<String, Object> fm = new HashMap<>();
                            fm.put("id",                f.getId());
                            fm.put("nomFichierOriginal", f.getNomFichierOriginal());
                            fm.put("nomFichierServeur",  f.getNomFichierServeur());
                            fm.put("typeMime",           f.getTypeMime() != null ? f.getTypeMime() : "");
                            fm.put("tailleFichier",      f.getTailleFichier());
                            fm.put("dateUpload",         f.getDateUpload());
                            fichiersList.add(fm);
                        }
                    }
                    item.put("commentaire",    r.getCommentaire());
                    item.put("soumisePar",     r.getSoumisePar());
                    item.put("dateSoumission", r.getDateSoumission());
                });
                item.put("fichiers", fichiersList);

                resultats.add(item);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("numeroDossier", dossier.getNumeroDossier());
            response.put("libelle",       dossier.getLibelle());
            response.put("resultats",     resultats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur resultats-prestataires: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPER PRIVÉ
    // ══════════════════════════════════════════════════════════════

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}