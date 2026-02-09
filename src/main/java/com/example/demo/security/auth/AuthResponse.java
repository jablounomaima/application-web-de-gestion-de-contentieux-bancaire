package com.example.demo.security.auth;

public class AuthResponse {
    private String username;
    private String token;

    public AuthResponse(String username, String token) {
        this.username = username;
        this.token = token;
    }

    // getters & setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
