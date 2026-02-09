package com.example.demo.security.service;

import com.example.demo.user.entity.Utilisateur;
import com.example.demo.user.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository repo;

    public CustomUserDetailsService(UtilisateurRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Utilisateur user = repo.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("❌ USER NOT FOUND: " + username);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + username);
                });
        
        System.out.println("✅ USER FOUND: " + username + " | Role: " + user.getRole() + " | PassHash: " + user.getPassword());

        String role = user.getRole().name(); // Déjà ROLE_XXX

        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
