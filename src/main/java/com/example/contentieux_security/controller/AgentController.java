package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.AgentProfileUpdateRequest;
import com.example.contentieux_security.dto.PasswordChangeRequest;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.entity.Client;
import com.example.contentieux_security.entity.DossierContentieux;
import com.example.contentieux_security.entity.Prestataire;
import com.example.contentieux_security.enums.TypePrestataire;
import com.example.contentieux_security.service.AgentBancaireService;
import com.example.contentieux_security.service.ClientService;
import com.example.contentieux_security.service.DossierService;
import com.example.contentieux_security.service.PrestataireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
@RequiredArgsConstructor
public class AgentController {

    private final AgentBancaireService agentService;
    private final PrestataireService prestataireService;
    private final ClientService clientService;
    private final DossierService dossierService;

    // ══════════════════════════════════════════════════════════════
    //  DASHBOARD
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public String agentDashboard(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        AgentBancaire agent = agentService.findAgentByUsername(oidcUser.getPreferredUsername());
        if (agent == null) {
            model.addAttribute("error", "Compte Keycloak non enregistré en base. Contactez un administrateur.");
            return "error";
        }
        model.addAttribute("agent", agent);
        model.addAttribute("agence", agent.getAgence());
        model.addAttribute("givenName", oidcUser.getGivenName());
        model.addAttribute("familyName", oidcUser.getFamilyName());
        model.addAttribute("username", oidcUser.getPreferredUsername());
        return "agent/dashboard";
    }

    // ══════════════════════════════════════════════════════════════
    //  CLIENTS
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/clients")
    public String gererClients(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String agentUsername = oidcUser.getPreferredUsername();
        AgentBancaire agent = agentService.findAgentByUsername(agentUsername);

        if (agent == null) {
            model.addAttribute("error", "Agent non trouvé");
            return "error";
        }

        if (agent.getAgence() == null) {
            model.addAttribute("error", "Agent non rattaché à une agence");
            return "error";
        }

        List<Client> clients = clientService.findByAgence(agent.getAgence());

        model.addAttribute("pageTitle", "Gérer les Clients");
        model.addAttribute("clients", clients != null ? clients : new ArrayList<>());
        model.addAttribute("nouveauClient", new Client());
        model.addAttribute("givenName", oidcUser.getGivenName());
        model.addAttribute("familyName", oidcUser.getFamilyName());
        model.addAttribute("username", agentUsername);
        return "agent/clients";
    }

    @PostMapping("/clients/creer")
    public String creerClient(@ModelAttribute Client nouveauClient,
                              @AuthenticationPrincipal OidcUser oidcUser,
                              RedirectAttributes redirectAttrs) {
        try {
            String agentUsername = oidcUser.getPreferredUsername();
            AgentBancaire agent = agentService.findAgentByUsername(agentUsername);

            if (agent == null || agent.getAgence() == null) {
                redirectAttrs.addFlashAttribute("error", "Agent ou agence non trouvé");
                return "redirect:/agent/clients";
            }

            nouveauClient.setAgence(agent.getAgence());
            nouveauClient.setDateInscription(LocalDate.now());

            clientService.save(nouveauClient);

            redirectAttrs.addFlashAttribute("success", "Client créé avec succès !");
            return "redirect:/agent/clients";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/agent/clients";
        }
    }

    @GetMapping("/clients/{id}/dossiers")
    public String voirDossiersClient(@PathVariable Long id,
                                     Model model,
                                     @AuthenticationPrincipal OidcUser oidcUser) {
        try {
            String agentUsername = oidcUser.getPreferredUsername();
            AgentBancaire agent = agentService.findAgentByUsername(agentUsername);

            if (agent == null) {
                model.addAttribute("error", "Agent non trouvé");
                return "error";
            }

            if (agent.getAgence() == null) {
                model.addAttribute("error", "Agent non rattaché à une agence");
                return "error";
            }

            Long agentAgenceId = agent.getAgence().getId();

            Client client = clientService.findById(id);

            if (client == null) {
                model.addAttribute("error", "Client non trouvé avec l'id: " + id);
                return "error";
            }

            if (client.getAgence() == null) {
                model.addAttribute("error", "Client " + id + " non rattaché à une agence");
                return "error";
            }

            Long clientAgenceId = client.getAgence().getId();

            if (!clientAgenceId.equals(agentAgenceId)) {
                model.addAttribute("error", "Client non autorisé pour cette agence");
                return "error";
            }

            List<DossierContentieux> dossiers = dossierService.findByClientId(id);

            model.addAttribute("client", client);
            model.addAttribute("dossiers", dossiers != null ? dossiers : new ArrayList<>());
            model.addAttribute("pageTitle", "Dossiers de " + client.getNom() + " " + client.getPrenom());
            model.addAttribute("givenName", oidcUser.getGivenName());
            model.addAttribute("familyName", oidcUser.getFamilyName());
            model.addAttribute("username", agentUsername);

            return "agent/clients/dossiers";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Erreur technique: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/missions")
    public String suivreMissions(Model model) {
        model.addAttribute("pageTitle", "Suivre les Missions");
        return "agent/missions";
    }

    // ══════════════════════════════════════════════════════════════
    //  MOT DE PASSE
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordChange", new PasswordChangeRequest());
        return "agent/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequest request,
                                 RedirectAttributes redirectAttrs) {
        try {
            agentService.changePassword(getCurrentUsername(), request);
            redirectAttrs.addFlashAttribute("success", "Mot de passe changé !");
            return "redirect:/agent/dashboard";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/change-password";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROFIL
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {
        AgentBancaire agent = agentService.findAgentByUsername(getCurrentUsername());
        if (agent == null) {
            model.addAttribute("error", "Agent non trouvé");
            return "error";
        }
        AgentProfileUpdateRequest request = new AgentProfileUpdateRequest();
        request.setNom(agent.getNom());
        request.setPrenom(agent.getPrenom());
        request.setEmail(agent.getEmail());
        request.setTelephone(agent.getTelephone());
        model.addAttribute("profileUpdate", request);
        model.addAttribute("agent", agent);
        return "agent/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute AgentProfileUpdateRequest request,
                                RedirectAttributes redirectAttrs) {
        try {
            agentService.updateProfile(getCurrentUsername(), request);
            redirectAttrs.addFlashAttribute("success", "Profil mis à jour !");
            return "redirect:/agent/dashboard";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/profile/edit";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PRESTATAIRES
    // ══════════════════════════════════════════════════════════════

      // ══════════════════════════════════════════════════════════════
    //  PRESTATAIRES
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/prestataires")
    public String listPrestataires(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String agentUsername = oidcUser.getPreferredUsername();
        List<Prestataire> prestataires = prestataireService.getPrestatairesParAgent(agentUsername);

        model.addAttribute("prestataires", prestataires != null ? prestataires : new ArrayList<>());
        model.addAttribute("nouveauPrestataire", new PrestataireCreationRequest());
        model.addAttribute("typesPrestataire", TypePrestataire.values());
        model.addAttribute("givenName", oidcUser.getGivenName());
        model.addAttribute("familyName", oidcUser.getFamilyName());
        model.addAttribute("username", agentUsername);
        return "agent/prestataires/list";
    }

    @GetMapping("/prestataires/create")
    public String showCreateForm(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return "redirect:/oauth2/authorization/keycloak";
        }

        model.addAttribute("nouveauPrestataire", new PrestataireCreationRequest());
        model.addAttribute("typesPrestataire", TypePrestataire.values());

        model.addAttribute("givenName", oidcUser.getGivenName() != null ? oidcUser.getGivenName() : "");
        model.addAttribute("familyName", oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "");
        model.addAttribute("username", oidcUser.getPreferredUsername() != null ? oidcUser.getPreferredUsername() : "");

        return "agent/prestataires/create";
    }

  // Redirection pour les anciens liens en français (utile pendant la transition)
  @GetMapping("/agent/prestataires/creer")
public String showCreateForm(Model model) {
    model.addAttribute("prestataire", new PrestataireCreationRequest());
    model.addAttribute("typesPrestataire", TypePrestataire.values());
    return "agent/prestataires/create";
}

    @PostMapping("/prestataires/create")        // ← CHANGÉ : "create" au lieu de "creer"
    public String creerPrestataire(@ModelAttribute("nouveauPrestataire") PrestataireCreationRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   RedirectAttributes redirectAttrs) {
        try {
            Prestataire p = prestataireService.creerPrestataire(request, oidcUser.getPreferredUsername());
            redirectAttrs.addFlashAttribute("success",
                "Prestataire '" + p.getPrenom() + " " + p.getNom()
                + "' créé ! Login: " + p.getUsername());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/agent/prestataires";
    }




        // ====================== ÉDITION D'UN PRESTATAIRE ======================
    
        @GetMapping("/prestataires/{id}/edit")
        public String editPrestataire(@PathVariable("id") Long id,
                                      Model model,
                                      @AuthenticationPrincipal OidcUser oidcUser) {
        
            Prestataire prestataire = prestataireService.findById(id);
        
            if (prestataire == null) {
                return "redirect:/agent/prestataires?error=Prestataire non trouvé";
            }
        
            PrestataireCreationRequest request = new PrestataireCreationRequest();
            request.setId(prestataire.getId());                    // ← AJOUTE CETTE LIGNE
            request.setUsername(prestataire.getUsername());
            request.setPrenom(prestataire.getPrenom());
            request.setNom(prestataire.getNom());
            request.setEmail(prestataire.getEmail());
            request.setTelephone(prestataire.getTelephone());
            request.setSpecialite(prestataire.getSpecialite());
            request.setNumeroCartePro(prestataire.getNumeroCartePro());
            request.setAdresse(prestataire.getAdresse());
            request.setTypePrestataire(prestataire.getType());           // ← Ici
            request.setNiveauValidation(prestataire.getNiveauValidation());
            request.setPlafondValidation(prestataire.getPlafondValidation());
        
            model.addAttribute("prestataire", request);
            model.addAttribute("typesPrestataire", TypePrestataire.values());
            model.addAttribute("givenName", oidcUser != null ? oidcUser.getGivenName() : "");
            model.addAttribute("familyName", oidcUser != null ? oidcUser.getFamilyName() : "");
            model.addAttribute("username", oidcUser != null ? oidcUser.getPreferredUsername() : "");
        
            return "agent/prestataires/form-edit";
        }
    
        @PostMapping("/prestataires/{id}/edit")
        public String updatePrestataire(@PathVariable("id") Long id,
                                        @ModelAttribute("prestataire") PrestataireCreationRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        RedirectAttributes redirectAttrs) {
            try {
                prestataireService.updatePrestataire(id, request, oidcUser.getPreferredUsername());
                redirectAttrs.addFlashAttribute("success", "Prestataire mis à jour avec succès !");
            } catch (Exception e) {
                redirectAttrs.addFlashAttribute("error", "Erreur lors de la mise à jour : " + e.getMessage());
            }
            return "redirect:/agent/prestataires";
        }


            // ====================== SUPPRESSION D'UN PRESTATAIRE ======================

    @PostMapping("/prestataires/{id}/delete")
    public String deletePrestataire(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes,
                                    @AuthenticationPrincipal OidcUser oidcUser) {
        
        try {
            boolean deleted = prestataireService.deletePrestataire(id, oidcUser.getPreferredUsername());
            
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Prestataire supprimé avec succès !");
            } else {
                redirectAttributes.addFlashAttribute("error", "Impossible de supprimer ce prestataire.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression : " + e.getMessage());
        }
        
        return "redirect:/agent/prestataires";
    }



        // ====================== ACTIVATION / DÉSACTIVATION (TOGGLE) ======================

        @PostMapping("/prestataires/{id}/toggle")
        public String togglePrestataire(@PathVariable("id") Long id,
                                        RedirectAttributes redirectAttributes,
                                        @AuthenticationPrincipal OidcUser oidcUser) {
    
            try {
                Prestataire updated = prestataireService.toggleActif(id, oidcUser.getPreferredUsername());
                String status = updated.isActif() ? "activé" : "désactivé";
                
                redirectAttributes.addFlashAttribute("success", 
                    "Prestataire " + updated.getPrenom() + " " + updated.getNom() 
                    + " a été " + status + " avec succès !");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            }
    
            return "redirect:/agent/prestataires";
        }

    // ... le reste de tes méthodes reste identique
    // ══════════════════════════════════════════════════════════════
    //  UTILITAIRE
    // ══════════════════════════════════════════════════════════════

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}