package com.example.contentieux_security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ValidateurController {

    @GetMapping("/validateur/dashboard-financier")
    public String dashboardFinancier() {
        return "validateur/dashboard-financier"; // sans .html
    }

    @GetMapping("/validateur/dashboard-juridique")
    public String dashboardJuridique() {
        return "validateur/dashboard-juridique";
    }
}