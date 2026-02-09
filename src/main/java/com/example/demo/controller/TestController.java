package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Ceci est un endpoint public");
        response.put("status", "success");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
    
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Données reçues avec succès");
        response.put("received", request);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }
    
    @GetMapping("/secured")
    public Map<String, String> securedEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Ceci est un endpoint sécurisé");
        response.put("status", "success");
        response.put("access", "Authentification requise");
        return response;
    }
}