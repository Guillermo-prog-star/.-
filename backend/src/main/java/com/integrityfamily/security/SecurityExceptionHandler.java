package com.integrityfamily.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.common.dto.ErrorResponse;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException; import java.time.LocalDateTime;
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
    @Override
    public void commence(HttpServletRequest rq, HttpServletResponse rs, AuthenticationException e) throws IOException {
        write(rs, 401, "No autenticado");
    }
    @Override
    public void handle(HttpServletRequest rq, HttpServletResponse rs, AccessDeniedException e) throws IOException {
        write(rs, 403, "Acceso denegado");
    }
    private void write(HttpServletResponse rs, int code, String msg) throws IOException {
        rs.setStatus(code); rs.setContentType("application/json"); rs.setCharacterEncoding("UTF-8");
        om.writeValue(rs.getWriter(), new ErrorResponse(false, msg, LocalDateTime.now(), null));
    }
}
