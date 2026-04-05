package com.integrityfamily.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class AuthJwtService {

    // Llave secreta temporal para desarrollo (Nodo Armenia)
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"));
    private static final long EXPIRATION_TIME = 864_000_000; // 10 días

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }
}