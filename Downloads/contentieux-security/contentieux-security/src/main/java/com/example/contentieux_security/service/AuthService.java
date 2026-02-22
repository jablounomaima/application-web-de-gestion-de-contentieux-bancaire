package com.example.contentieux_security.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final String SECRET = "a-very-long-and-secure-secret-key-for-contentieux-security-123456";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
    private final long EXPIRATION_TIME = 86400000; // 1 day

    public String generateToken(String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();

        // Mock Keycloak structure
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        claims.put("realm_access", realmAccess);
        claims.put("preferred_username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Key getSigningKey() {
        return key;
    }
}
