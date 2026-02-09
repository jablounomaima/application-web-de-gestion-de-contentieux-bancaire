package com.example.demo.user.controller;

import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Créer un nouvel utilisateur (intervenant)
     * Accessible uniquement par l'AGENT_BANCAIRE
     */
    @PostMapping
    @PreAuthorize("hasRole('AGENT_BANCAIRE')")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserResponse response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lister tous les utilisateurs
     * Accessible par AGENT_BANCAIRE et ADMIN
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT_BANCAIRE', 'ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Obtenir un utilisateur par ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT_BANCAIRE', 'ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Supprimer un utilisateur
     * Accessible uniquement par l'ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
