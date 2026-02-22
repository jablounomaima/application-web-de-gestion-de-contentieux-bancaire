package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expert")
public class ExpertController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('EXPERT')")
    public String expertDashboard() {
        return "Espace Expert : Rapports d'expertise et évaluations";
    }
}
