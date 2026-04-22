package com.example.contentieux_security.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    @GetMapping("/")
    public ResponseEntity<?> index() {
        return ResponseEntity.ok(Map.of("message", "API Contentieux Security"));
    }

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok(Map.of("message", "Bienvenue sur l'API"));
    }
}
