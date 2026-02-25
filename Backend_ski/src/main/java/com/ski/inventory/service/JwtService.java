package com.ski.inventory.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Service pro práci s JWT tokeny
 */
@Service
public class JwtService {
    
    private final SecretKey secretKey;
    private final long expiration;
    
    public JwtService(
            @Value("${jwt.secret:your-256-bit-secret-key-change-this-in-production-minimum-32-characters}") String secret,
            @Value("${jwt.expiration:86400000}") long expiration) {
        // Vytvořit SecretKey z stringu (minimálně 32 znaků)
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    
    /**
     * Vytvoří JWT token pro uživatele
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }
    
    /**
     * Vytvoří JWT token s custom claims
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    /**
     * Vytvoří JWT token s vlastní expirací (pro Swagger entry cookie, order view link, atd.)
     */
    public String createTokenWithCustomExpiry(Map<String, Object> claims, String subject, long expirationMs) {
        return createToken(claims, subject, expirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Krátkodobý token pro přístup ke Swagger UI (cookie, platnost 5 min)
     */
    public String generateSwaggerEntryToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        return createToken(claims, "swagger", 5 * 60 * 1000L);
    }
    
    /**
     * Extrahuje username z tokenu
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrahuje roli z tokenu
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    
    /**
     * Extrahuje expiration date z tokenu
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extrahuje konkrétní claim z tokenu
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extrahuje všechny claims z tokenu
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Ověří, zda je token platný a neexpirovaný
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
    
    /**
     * Zkontroluje, zda je token expirovaný
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Bezpečně zparsuje token a vrátí claims, nebo prázdný Optional při chybě/expiraci.
     */
    public Optional<Claims> parseTokenSafe(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().before(new Date())) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
