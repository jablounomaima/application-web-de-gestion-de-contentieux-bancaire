package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/huissier")
public class HuissierController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('HUISSIER')")
    public String huissierDashboard() {
        return "Espace Huissier : Gestion des notifications et exécutions";
    }
}
