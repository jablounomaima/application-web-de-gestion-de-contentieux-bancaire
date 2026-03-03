package com.example.contentieux_security.controller;

import com.example.contentieux_security.dto.*;
import com.example.contentieux_security.service.AgenceService;
import com.example.contentieux_security.service.AgentBancaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AgenceService agenceService;
    private final AgentBancaireService agentService;

    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("totalAgences", agenceService.getAllAgences().size());
        model.addAttribute("totalAgents", agentService.getAllAgents().size());
        return "admin/dashboard";
    }

    // ==================== AGENCES ====================

    @GetMapping("/agences")
    public String listAgences(Model model) {
        List<AgenceDTO> agences = agenceService.getAllAgences();
        model.addAttribute("agences", agences);
        model.addAttribute("agence", new AgenceDTO()); // Pour le formulaire
        return "admin/agences";
    }

    @PostMapping("/agences")
    public String createAgence(@ModelAttribute AgenceDTO agenceDTO, 
                               RedirectAttributes redirectAttrs) {
        try {
            agenceService.createAgence(agenceDTO);
            redirectAttrs.addFlashAttribute("success", "Agence créée avec succès !");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    @PostMapping("/agences/{id}/update")
    public String updateAgence(@PathVariable Long id, 
                               @ModelAttribute AgenceDTO agenceDTO,
                               RedirectAttributes redirectAttrs) {
        try {
            agenceService.updateAgence(id, agenceDTO);
            redirectAttrs.addFlashAttribute("success", "Agence mise à jour !");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    @PostMapping("/agences/{id}/delete")
    public String deleteAgence(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            agenceService.deleteAgence(id);
            redirectAttrs.addFlashAttribute("success", "Agence supprimée !");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/agences";
    }

    // ==================== AGENTS ====================
// ==================== AGENTS ====================

@GetMapping("/agents")
public String listAgents(Model model) {
    List<AgentBancaireDTO> agents = agentService.getAllAgents();
    List<AgenceDTO> agences = agenceService.getAllAgences();

    model.addAttribute("agents", agents);
    model.addAttribute("agences", agences);
    model.addAttribute("agent", new AgentCreationRequest());
    return "admin/agents";
}

@PostMapping("/agents")
public String createAgent(@ModelAttribute AgentCreationRequest request,
                          RedirectAttributes redirectAttrs) {
    try {
        agentService.createAgent(request);
        redirectAttrs.addFlashAttribute("success", "Agent créé avec succès !");
    } catch (Exception e) {
        redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/admin/agents";
}

@PostMapping("/agents/{id}/update")
public String updateAgent(@PathVariable Long id,
                          @ModelAttribute AgentCreationRequest request,
                          RedirectAttributes redirectAttrs) {
    try {
        agentService.updateAgent(id, request);
        redirectAttrs.addFlashAttribute("success", "Agent mis à jour avec succès !");
    } catch (Exception e) {
        redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/admin/agents";
}

@PostMapping("/agents/{id}/delete")
public String deleteAgent(@PathVariable Long id,
                          RedirectAttributes redirectAttrs) {
    try {
        agentService.deleteAgent(id);
        redirectAttrs.addFlashAttribute("success", "Agent supprimé avec succès !");
    } catch (Exception e) {
        redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/admin/agents";
}

@PostMapping("/agents/{id}/toggle")
public String toggleAgentStatus(@PathVariable Long id,
                                RedirectAttributes redirectAttrs) {
    try {
        agentService.toggleAgentStatus(id);
        redirectAttrs.addFlashAttribute("success", "Statut modifié !");
    } catch (Exception e) {
        redirectAttrs.addFlashAttribute("error", "Erreur : " + e.getMessage());
    }
    return "redirect:/admin/agents";
}

}