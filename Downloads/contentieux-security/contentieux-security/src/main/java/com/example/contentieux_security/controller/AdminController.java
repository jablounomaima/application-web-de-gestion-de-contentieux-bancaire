package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard() {
        return "Espace Admin";
    }

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminHello() {
        return "Hello admin!";
    }
}