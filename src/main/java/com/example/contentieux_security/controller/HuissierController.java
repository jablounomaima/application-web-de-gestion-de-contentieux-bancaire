package com.example.contentieux_security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;

@Controller
@RequestMapping("/huissier")
public class HuissierController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("mandatsPrioritaires", new ArrayList<>());
        model.addAttribute("mandatsActifs", 10);
        model.addAttribute("significations", 5);
        model.addAttribute("saisiesEnCours", 2);
        model.addAttribute("montantRecouvre", "20000");

        return "huissier/dashboard";
    }
}