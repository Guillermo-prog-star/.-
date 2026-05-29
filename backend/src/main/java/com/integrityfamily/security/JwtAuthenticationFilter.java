// backend/src/main/java/com/integrityfamily/security/JwtAuthenticationFilter.java
package com.integrityfamily.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    // ── Paths públicos: el filtro los salta completamente ───────────────────
    // FIX: agregado /auth/ (sin /api) para cubrir ambos mappings posibles
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/",
            "/api/auth/",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/ws/"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            log.debug("[JWT-FILTER] Path público, sin validación JWT: {}", path);
        }
        return isPublic;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractJwtFromRequest(request);

            if (token != null) {
                Claims claims = jwtTokenProvider.parse(token);
                String email = claims.getSubject();

                // Propagar familyId al TenantContext para multi-tenancy
                Number fid = claims.get("fid", Number.class);
                if (fid != null) {
                    TenantContext.setCurrentFamilyId(fid.longValue());
                }

                if (email != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        } catch (ExpiredJwtException ex) {
            // ── FIX CENTRAL ─────────────────────────────────────────────────
            // Token expirado → SecurityContext vacío, pero la cadena CONTINÚA.
            // Spring Security devolverá 401 en endpoints protegidos.
            // Endpoints públicos (ej. /auth/login) ignoran el contexto vacío.
            // ANTES: el catch no llamaba filterChain.doFilter() → 403 silencioso.
            log.warn("[JWT-FILTER] Token expirado en {}: {}",
                    request.getServletPath(), ex.getMessage());
            SecurityContextHolder.clearContext();

        } catch (JwtException ex) {
            // Token malformado o firma inválida → mismo tratamiento
            log.warn("[JWT-FILTER] Token JWT inválido en {}: {}",
                    request.getServletPath(), ex.getMessage());
            SecurityContextHolder.clearContext();

        } catch (Exception ex) {
            // Cualquier otro error inesperado → log completo para diagnóstico
            log.error("[JWT-FILTER] Error inesperado procesando JWT en {}: {}",
                    request.getServletPath(), ex.getMessage());
            SecurityContextHolder.clearContext();

        } finally {
            // Siempre limpiar TenantContext para evitar fugas entre hilos del pool
            TenantContext.clear();
        }

        // ── FIX: filterChain.doFilter() SIEMPRE se ejecuta, fuera del try ──
        // Antes estaba dentro del try → un catch lo cortocircuitaba.
        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}