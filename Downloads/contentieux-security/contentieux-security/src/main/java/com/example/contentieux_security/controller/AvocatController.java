package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avocat")
public class AvocatController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('AVOCAT')")
    public String avocatDashboard() {
        return "Espace Avocat : Suivi des affaires juridiques";
    }
}
