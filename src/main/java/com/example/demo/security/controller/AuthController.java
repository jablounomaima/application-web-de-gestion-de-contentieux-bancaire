package com.example.demo.security.controller;

import com.example.demo.security.auth.AuthRequest;
import com.example.demo.security.auth.AuthResponse;
import com.example.demo.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.example.demo.user.repository.UtilisateurRepository utilisateurRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, 
                          JwtService jwtService,
                          com.example.demo.user.repository.UtilisateurRepository utilisateurRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= API REGISTER =================
    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        if (utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Utilisateur existe déjà");
        }

        com.example.demo.user.entity.Utilisateur user = new com.example.demo.user.entity.Utilisateur();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Par défaut, on attribue un rôle (ici AGENT_BANCAIRE pour l'exemple, ou via la requête)
        user.setRole(com.example.demo.user.role.Role.ROLE_AGENT_BANCAIRE); 

        utilisateurRepository.save(user);

        // On génère le token directement pour le nouvel utilisateur
        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(), 
                user.getPassword(), 
                java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().name()))
        );

        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(user.getUsername(), token);
    }

    // ================= API LOGIN =================
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        try {
            // Authentification
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Récupérer l'objet UserDetails
            UserDetails user = (UserDetails) auth.getPrincipal();

            // Génération du JWT avec UserDetails
            String token = jwtService.generateToken(user);

            // Retourner token + username
            return new AuthResponse(user.getUsername(), token);

        } catch (BadCredentialsException ex) {
            System.out.println("❌ ECHEC AUTHENTIFICATION pour " + request.getUsername());
            throw new RuntimeException("Nom d'utilisateur ou mot de passe incorrect");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur technique: " + e.getMessage());
        }
    }

    // ❌ SUPPRIMER CETTE MÉTHODE pour éviter le conflit
    // @GetMapping("/login")
    // public String loginPage() {
    //     return "login";
    // }
}