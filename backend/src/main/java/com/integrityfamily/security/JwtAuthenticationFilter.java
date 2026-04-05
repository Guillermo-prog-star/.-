package com.integrityfamily.security;

import com.integrityfamily.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter: Componente encargado de interceptar cada petición HTTP,
 * extraer el token JWT, validarlo y establecer la autenticación en el contexto de seguridad.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Validación de cabecera: Si no existe o no tiene el formato Bearer, dejar pasar (PermitAll)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extracción segura del token
        jwt = authHeader.substring(7);

        try {
            // 3. Extracción del nombre de usuario (Email) desde el token
            userEmail = jwtService.extractUsername(jwt);

            // 4. Verificación de usuario y estado de autenticación actual
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Carga de detalles del usuario desde la base de datos
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 5. Validación de integridad y expiración del token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    
                    // Enlaza detalles adicionales de la petición (IP, Sesión)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 6. Establecer el contexto de seguridad para Spring Boot
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Error de parsing o expiración: Se ignora para permitir que SecurityConfig 
            // decida si la ruta es pública o privada (401/403).
        }

        // 7. Continuar con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }
}