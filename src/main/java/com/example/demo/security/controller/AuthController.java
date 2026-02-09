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

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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
            throw new RuntimeException("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    // ❌ SUPPRIMER CETTE MÉTHODE pour éviter le conflit
    // @GetMapping("/login")
    // public String loginPage() {
    //     return "login";
    // }
}