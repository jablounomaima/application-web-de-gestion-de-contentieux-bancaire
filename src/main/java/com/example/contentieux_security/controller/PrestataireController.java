package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/prestataire")
public class PrestataireController {

    @GetMapping("/avocat/dashboard")
    @PreAuthorize("hasRole('AVOCAT')")
    public String avocatDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Avocat");
        model.addAttribute("specialite", "Représentation en justice");
        return "prestataire/avocat-dashboard";
    }

    @GetMapping("/expert/dashboard")
    @PreAuthorize("hasRole('EXPERT')")
    public String expertDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Expert Judiciaire");
        model.addAttribute("specialite", "Évaluation immobilière");
        return "prestataire/expert-dashboard";
    }

    @GetMapping("/huissier/dashboard")
    @PreAuthorize("hasRole('HUISSIER')")
    public String huissierDashboard(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        model.addAttribute("role", "Huissier de Justice");
        model.addAttribute("specialite", "Signification et saisie");
        return "prestataire/huissier-dashboard";
    }

    @GetMapping("/missions")
    @PreAuthorize("hasAnyRole('AVOCAT', 'EXPERT', 'HUISSIER')")
    public String consulterMissions(Model model, Principal principal) {
        model.addAttribute("pageTitle", "Mes Missions");
        model.addAttribute("username", principal.getName());
        return "prestataire/missions";
    }

    @GetMapping("/mission/remplir")
    @PreAuthorize("hasAnyRole('AVOCAT', 'EXPERT', 'HUISSIER')")
    public String remplirMission(Model model) {
        model.addAttribute("pageTitle", "Remplir Résultat de Mission");
        return "prestataire/remplir-mission";
    }
}