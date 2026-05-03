package com.integrityfamily.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Manejador centralizado de errores de seguridad (401 y 403).
 * Optimizado para usar el ObjectMapper de Spring y constantes de HttpStatus.
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Inyectamos el bean de Spring para asegurar consistencia en la serializaciÃƒÂ³n de fechas
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, 
                         HttpServletResponse response, 
                         AuthenticationException authException) throws IOException {
        writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "No autenticado: Se requiere token vÃƒÂ¡lido");
    }

    @Override
    public void handle(HttpServletRequest request, 
                       HttpServletResponse response, 
                       AccessDeniedException accessDeniedException) throws IOException {
        writeErrorResponse(response, HttpStatus.FORBIDDEN, "Acceso denegado: Permisos insuficientes");
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse error = new ErrorResponse(
                false, 
                message, 
                LocalDateTime.now(), 
                null
        );

        objectMapper.writeValue(response.getWriter(), error);
    }
}


