package com.integrityfamily.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtService: Arquitectura Criptográfica Maestra para Integrity Family.
 * Implementación optimizada para JJWT 0.12.6 y Spring Boot 3.4.3.
 * REDISEÑO: Eliminación de Map<String,Object> y secretos hardcodeados.
 */
@Service
public class JwtService {

    @Value("${integrity.security.jwt.secret}")
    private String secretKey;

    @Value("${integrity.security.jwt.expiration-ms:86400000}") // 24 Horas
    private long jwtExpiration;

    // DTO Tipado para evitar el antipatrón Map<String,Object>
    public record CustomClaims(Long familyId, String role) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (familyId != null) map.put("familyId", familyId);
            if (role != null) map.put("role", role);
            return map;
        }
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.JwtException e) {
            // Token expirado, mal formado o firma inválida → no válido
            return false;
        }
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new CustomClaims(null, null), userDetails);
    }

    /**
     * Genera un token JWT con claims controlados por el DTO CustomClaims.
     */
    public String generateToken(CustomClaims extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims.toMap()) // La conversión a mapa es interna, no expuesta en la firma
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        // Validación fail-fast si la clave no fue inyectada
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("CRITICAL: JWT Secret Key not configured in environment variables.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
