package com.example.contentieux_security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/validation")
public class ValidationController {

    @GetMapping("/juridique")
    @PreAuthorize("hasRole('VALID_JURIDIQUE')")
    public String validationJuridique() {
        return "Validation Juridique";
    }

    @GetMapping("/financier")
    @PreAuthorize("hasRole('VALID_FINANCIER')")
    public String validationFinanciere() {
        return "Validation Financière";
    }
}