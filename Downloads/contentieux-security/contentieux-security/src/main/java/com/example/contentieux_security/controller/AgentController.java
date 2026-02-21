package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('AGENT')")
    public String agentDashboard() {
        return "Espace Agent";
    }
}