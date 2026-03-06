package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.PrestataireCreationRequest;
import com.example.contentieux_security.dto.PrestataireDTO;
import com.example.contentieux_security.entity.AgentBancaire;
import com.example.contentieux_security.service.AgentBancaireService;
import com.example.contentieux_security.service.PrestataireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/agent/prestataires")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class AgentPrestataireController {

    private final PrestataireService prestataireService;
    private final AgentBancaireService agentService;

    /**
     * Liste tous les prestataires gérés par l'agent
     */
    @GetMapping
    public String listPrestataires(Model model) {
        String username = getCurrentUsername();
        AgentBancaire agent = agentService.findAgentByUsername(username);
        
        if (agent == null) {
            model.addAttribute("error", "Agent non trouvé");
            return "error";
        }

        List<PrestataireDTO> prestataires = prestataireService.getPrestatairesByAgent(agent.getId());
        
        model.addAttribute("prestataires", prestataires);
        model.addAttribute("agent", agent);
        
        return "agent/prestataires/list";
    }

    /**
     * Filtrer par type
     */
    @GetMapping("/type/{type}")
    public String listByType(@PathVariable String type, Model model) {
        String username = getCurrentUsername();
        AgentBancaire agent = agentService.findAgentByUsername(username);
        
        List<PrestataireDTO> prestataires = prestataireService.getPrestatairesByTypeAndAgent(type, agent.getId());
        
        model.addAttribute("prestataires", prestataires);
        model.addAttribute("typeFiltre", type);
        model.addAttribute("agent", agent);
        
        return "agent/prestataires/list";
    }

    /**
     * Formulaire de création
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("prestataire", new PrestataireCreationRequest());
        model.addAttribute("types", new String[]{
            "AVOCAT", "HUISSIER", "EXPERT", "VALIDATEUR_FINANCIER", "VALIDATEUR_JURIDIQUE"
        });
        return "agent/prestataires/create";
    }

    /**
     * Créer un prestataire
     */
    @PostMapping("/create")
    public String createPrestataire(@ModelAttribute PrestataireCreationRequest request,
                                   RedirectAttributes redirectAttrs) {
        String agentUsername = getCurrentUsername();
        
        try {
            prestataireService.createPrestataire(request, agentUsername);
            redirectAttrs.addFlashAttribute("success", 
                "Prestataire " + request.getType() + " créé avec succès !");
            return "redirect:/agent/prestataires";
            
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/agent/prestataires/create";
        }
    }

    /**
     * Activer/Désactiver un prestataire
     */
    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        String agentUsername = getCurrentUsername();
        
        try {
            prestataireService.togglePrestataireStatus(id, agentUsername);
            redirectAttrs.addFlashAttribute("success", "Statut modifié avec succès");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/agent/prestataires";
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}