package com.example.contentieux_security.dto;

public class UtilisateurDto {
    private String prenom;
    private String nom;
    private String email;
    private String username;
    private String role;
    private String motDePasse;
    private boolean actif = true;
    private boolean forcerChangementMdp = true;

    // ── Getters & Setters ──
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public boolean isForcerChangementMdp() { return forcerChangementMdp; }
    public void setForcerChangementMdp(boolean f) { this.forcerChangementMdp = f; }
}