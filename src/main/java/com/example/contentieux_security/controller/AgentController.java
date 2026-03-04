package com.example.contentieux_security.controller;
import com.example.contentieux_security.dto.PasswordChangeRequest;  // AJOUTER

import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.service.AgentBancaireService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;  // AJOUTER
import org.springframework.web.bind.annotation.PostMapping;       // AJOUTER


@Controller
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AgentController {

    private final AgentBancaireService agentService;

    public AgentController(AgentBancaireService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/dashboard")
    public String agentDashboard(Model model, RedirectAttributes redirectAttrs) {
        // Récupérer le username depuis l'authentification Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        System.out.println("=== DASHBOARD AGENT ===");
        System.out.println("Username authentifié: " + username);
        System.out.println("Authorities: " + auth.getAuthorities());

        if (username == null || username.equals("anonymousUser")) {
            redirectAttrs.addFlashAttribute("error", "Session invalide - Veuillez vous reconnecter");
            return "redirect:/login";
        }

        try {
            // Récupérer l'agent depuis la base locale
            AgentBancaire agent = agentService.findAgentByUsername(username);
            
            if (agent == null) {
                System.out.println("❌ Agent non trouvé en base locale: " + username);
                model.addAttribute("error", "Votre compte Keycloak existe mais vous n'êtes pas enregistré dans la base locale. Contactez l'administrateur.");
                return "error";
            }

            System.out.println("✅ Agent trouvé: " + agent.getNom() + " " + agent.getPrenom());
            System.out.println("Agence: " + (agent.getAgence() != null ? agent.getAgence().getNom() : "null"));

            model.addAttribute("agent", agent);
            model.addAttribute("agence", agent.getAgence());
            model.addAttribute("username", username);

            return "agent/dashboard";

        } catch (Exception e) {
            System.out.println("❌ Erreur dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur système: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/dossiers")
    public String gererDossiers(Model model) {
        model.addAttribute("pageTitle", "Gérer les Dossiers");
        return "agent/dossiers";
    }

    @GetMapping("/clients")
    public String gererClients(Model model) {
        model.addAttribute("pageTitle", "Gérer les Clients");
        return "agent/clients";
    }

    @GetMapping("/prestataires")
    public String gererPrestataires(Model model) {
        model.addAttribute("pageTitle", "Gérer les Comptes Prestataires");
        return "agent/prestataires";
    }

    @GetMapping("/missions")
    public String suivreMissions(Model model) {
        model.addAttribute("pageTitle", "Suivre les Missions");
        return "agent/missions";
    }
    @GetMapping("/change-password")
public String showChangePasswordForm(Model model) {
    model.addAttribute("passwordChange", new PasswordChangeRequest());
    return "agent/change-password";
}

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute PasswordChangeRequest request,
                            RedirectAttributes redirectAttrs) {
    
    // Récupérer le username de l'agent connecté
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();
    
    try {
        agentService.changePassword(username, request);
        redirectAttrs.addFlashAttribute("success", "Mot de passe changé avec succès !");
        return "redirect:/agent/dashboard";
        
    } catch (Exception e) {
        redirectAttrs.addFlashAttribute("error", e.getMessage());
        return "redirect:/agent/change-password";
    }
}





}