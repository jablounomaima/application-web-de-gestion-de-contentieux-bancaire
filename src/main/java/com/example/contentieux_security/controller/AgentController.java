package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AgentController {

    @GetMapping("/dashboard")
    public String agentDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Agent Bancaire");
        return "agent/dashboard";
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
}