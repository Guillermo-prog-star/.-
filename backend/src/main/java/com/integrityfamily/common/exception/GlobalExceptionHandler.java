package com.integrityfamily.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyUsedException(EmailAlreadyUsedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("EMAIL_ALREADY_USED");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode(ex.getCode());
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());

        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("[VALIDATION] IllegalArgumentException at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("INVALID_ARGUMENT");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("[STATE] IllegalStateException at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("INVALID_STATE");
        error.setMessage(ex.getMessage());
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("VALIDATION_ERROR");
        error.setMessage("Error de validación: " + details);
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("[AUTH] Intento de acceso con credenciales inválidas en: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("INVALID_CREDENTIALS");
        error.setMessage("Credenciales inválidas. Verifique su email y contraseña.");
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(DisabledException ex, HttpServletRequest request) {
        log.warn("[AUTH] Cuenta deshabilitada: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("ACCOUNT_DISABLED");
        error.setMessage("La cuenta está deshabilitada. Contacte al administrador del Nodo.");
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedAccount(LockedException ex, HttpServletRequest request) {
        log.warn("[AUTH] Cuenta bloqueada: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("ACCOUNT_LOCKED");
        error.setMessage("La cuenta está bloqueada temporalmente por múltiples intentos fallidos.");
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[AUTH] Acceso denegado a: {}", request.getRequestURI());
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("ACCESS_DENIED");
        error.setMessage("No tiene permisos para acceder a este recurso del Nodo.");
        error.setPath(request.getRequestURI());
        error.setTimestamp(LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 🔥 CRÍTICO: excluir Actuator completamente
        if (path.startsWith("/actuator")) {
            log.error("Actuator Error at {}: {}", path, ex.getMessage());
            throw new RuntimeException(ex);
        }

        log.error("Unhandled Exception at {}: {} - {}", path, ex.getClass().getName(), ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse();
        error.setSuccess(false);
        error.setCode("INTERNAL_SERVER_ERROR");
        error.setMessage("Error interno en el sistema: " + ex.getMessage());
        error.setPath(path);
        error.setTimestamp(LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

