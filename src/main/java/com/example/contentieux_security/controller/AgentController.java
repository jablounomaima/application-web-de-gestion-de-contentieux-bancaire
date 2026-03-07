package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.PasswordChangeRequest;
import com.example.contentieux_security.dto.AgentProfileUpdateRequest;
import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;

import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.service.AgentBancaireService;
import com.example.contentieux_security.service.PrestataireService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AgentController {

    private final AgentBancaireService agentService;
    private final PrestataireService prestataireService;

    public AgentController(AgentBancaireService agentService,
                           PrestataireService prestataireService) {
        this.agentService = agentService;
        this.prestataireService = prestataireService;
    }

    @GetMapping("/dashboard")
    public String agentDashboard(Model model,
                                 @AuthenticationPrincipal OidcUser oidcUser) {
    
        // ── Données base locale ──
        AgentBancaire agent = agentService.findAgentByUsername(oidcUser.getPreferredUsername());
    
        if (agent == null) {
            model.addAttribute("error",
                "Compte Keycloak non enregistré en base. Contactez un administrateur.");
            return "error";
        }
    
        model.addAttribute("agent",      agent);
        model.addAttribute("agence",     agent.getAgence());
    
        // ── Données Keycloak pour le layout ──
        model.addAttribute("givenName",  oidcUser.getGivenName());
        model.addAttribute("familyName", oidcUser.getFamilyName());
        model.addAttribute("username",   oidcUser.getPreferredUsername());
    
        return "agent/dashboard";
    }

    // ================= DOSSIERS =================

    @GetMapping("/dossiers")
    public String gererDossiers(Model model) {
        model.addAttribute("pageTitle", "Gérer les Dossiers");
        return "agent/dossiers";
    }

    // ================= CLIENTS =================

    @GetMapping("/clients")
    public String gererClients(Model model) {
        model.addAttribute("pageTitle", "Gérer les Clients");
        return "agent/clients";
    }

    // ================= MISSIONS =================

    @GetMapping("/missions")
    public String suivreMissions(Model model) {
        model.addAttribute("pageTitle", "Suivre les Missions");
        return "agent/missions";
    }

    // ================= CHANGE PASSWORD =================

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordChange", new PasswordChangeRequest());
        return "agent/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequest request,
                                 RedirectAttributes redirectAttrs) {

        String username = getCurrentUsername();

        try {
            agentService.changePassword(username, request);
            redirectAttrs.addFlashAttribute("success", "Mot de passe changé !");
            return "redirect:/agent/dashboard";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/change-password";
        }
    }

    // ================= EDIT PROFILE =================

    @GetMapping("/profile/edit")
    public String showEditProfileForm(Model model) {

        String username = getCurrentUsername();
        AgentBancaire agent = agentService.findAgentByUsername(username);

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

        String username = getCurrentUsername();

        try {
            agentService.updateProfile(username, request);
            redirectAttrs.addFlashAttribute("success", "Profil mis à jour !");
            return "redirect:/agent/dashboard";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/profile/edit";
        }
    }

    // ================= PRESTATAIRES - EDIT =================

    @GetMapping("/prestataires/{id}/edit")
    public String showEditPrestataireForm(@PathVariable Long id,
                                          Model model,
                                          RedirectAttributes redirectAttrs) {

        String username = getCurrentUsername();

        try {
            PrestataireDTO prestataire =
                    prestataireService.getPrestataireByIdAndAgent(id, username);

            model.addAttribute("prestataire", prestataire);
            model.addAttribute("mode", "edit");

            return "agent/prestataires/form-edit";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/prestataires";
        }
    }

    @PostMapping("/prestataires/{id}/edit")
    public String updatePrestataire(@PathVariable Long id,
                                    @ModelAttribute PrestataireCreationRequest request,
                                    RedirectAttributes redirectAttrs) {

        String username = getCurrentUsername();

        try {
            prestataireService.updatePrestataire(id, request, username);
            redirectAttrs.addFlashAttribute("success", "Prestataire mis à jour !");
            return "redirect:/agent/prestataires";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/prestataires/" + id + "/edit";
        }
    }

    // ================= PRESTATAIRES - DELETE =================

    @PostMapping("/prestataires/{id}/delete")
    public String deletePrestataire(@PathVariable Long id,
                                    RedirectAttributes redirectAttrs) {

        String username = getCurrentUsername();

        try {
            prestataireService.deletePrestataire(id, username);
            redirectAttrs.addFlashAttribute("success", "Prestataire supprimé !");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/agent/prestataires";
    }

    // ================= UTILITAIRE =================

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}