// backend/src/main/java/com/integrityfamily/security/AuthExceptionHandler.java
package com.integrityfamily.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuthExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Ã¢Å“â€¦ ObjectMapper configurado correctamente para fechas ISO-8601
    private static final ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // Ã¢Å“â€¦ 401 - Token ausente o invÃƒÂ¡lido
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writeResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "No autenticado: " + authException.getMessage(),
                request.getRequestURI()
        );
    }

    // Ã¢Å“â€¦ 403 - Autenticado pero sin permisos suficientes
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writeResponse(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Acceso denegado: " + accessDeniedException.getMessage(),
                request.getRequestURI()
        );
    }

    private void writeResponse(
            HttpServletResponse response,
            int status,
            String message,
            String path
    ) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("status", status);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", LocalDateTime.now().toString()); // Ã¢Å“â€¦ "2025-03-17T10:30:00"

        objectMapper.writeValue(response.getWriter(), body); // Ã¢Å“â€¦ writeValue > writeValueAsString
    }
}


