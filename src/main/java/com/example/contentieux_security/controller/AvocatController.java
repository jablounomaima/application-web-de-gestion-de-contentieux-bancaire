package com.example.contentieux_security.controller;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AvocatController {

    @GetMapping("/avocat/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User user, Model model) {

        String firstName = user.getAttribute("given_name");
        String lastName = user.getAttribute("family_name");

        model.addAttribute("firstName", firstName);
        model.addAttribute("lastName", lastName);

        return "avocat/dashboard";
    }
}