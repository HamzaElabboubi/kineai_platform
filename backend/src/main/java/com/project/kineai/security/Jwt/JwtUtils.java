package com.project.kineai.security.Jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpiration;

    //---------- Cle de signature-----------
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ── Générer Access Token ──────────────────
    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Générer Refresh Token ─────────────────
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ------ Extraire Email du token-----
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // ── Extraire rôle du token ────────────────
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── Valider token ─────────────────────────
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expiré : {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT non supporté : {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformé : {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Erreur JWT : {}", e.getMessage());
        }
        return false;
    }

    // ── Extraire les claims du token ───────────
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
