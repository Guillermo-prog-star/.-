package com.integrityfamily.familyhome.api;

import com.integrityfamily.familyhome.application.exception.*;
import com.integrityfamily.hud.api.AdaptiveHudController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice(assignableTypes = {FamilyHomeController.class, FamilyActionController.class, AdaptiveHudController.class})
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public final class FamilyHomeExceptionHandler {

    private static final String MDC_KEY = "correlationId";

    @ExceptionHandler(FamilyHomeAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            FamilyHomeAccessDeniedException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Hogar familiar no disponible");
        problem.setDetail("No fue posible encontrar un hogar familiar accesible.");
        problem.setType(URI.create("https://integrityfamily/errors/family-home-not-found"));
        problem.setProperty("code", "FAMILY_HOME_NOT_FOUND");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(FamilyHomeUnauthenticatedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthenticated(
            FamilyHomeUnauthenticatedException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Autenticación requerida");
        problem.setDetail("Debe estar autenticado para acceder al hogar familiar.");
        problem.setType(URI.create("https://integrityfamily/errors/authentication-required"));
        problem.setProperty("code", "AUTHENTICATION_REQUIRED");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(FamilyHomeAuthenticationMappingException.class)
    public ResponseEntity<ProblemDetail> handleAuthMapping(
            FamilyHomeAuthenticationMappingException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Error de mapeo de autenticación");
        problem.setDetail("No fue posible verificar las credenciales del usuario en el sistema.");
        problem.setType(URI.create("https://integrityfamily/errors/authentication-required"));
        problem.setProperty("code", "AUTHENTICATION_REQUIRED");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(FamilyHomeDataIncompleteException.class)
    public ResponseEntity<ProblemDetail> handleDataIncomplete(
            FamilyHomeDataIncompleteException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Hogar familiar temporalmente no disponible");
        problem.setDetail("La información del hogar familiar se está procesando o se encuentra incompleta.");
        problem.setType(URI.create("https://integrityfamily/errors/family-home-temporarily-unavailable"));
        problem.setProperty("code", "FAMILY_HOME_TEMPORARILY_UNAVAILABLE");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(UnsupportedJourneyStageException.class)
    public ResponseEntity<ProblemDetail> handleStateConflict(
            UnsupportedJourneyStageException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Conflicto de estado del hogar familiar");
        problem.setDetail("El estado actual del viaje no es compatible con esta proyección.");
        problem.setType(URI.create("https://integrityfamily/errors/family-home-state-conflict"));
        problem.setProperty("code", "FAMILY_HOME_STATE_CONFLICT");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        if ("familyId".equals(ex.getName())) {
            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problem.setTitle("Identificador de familia inválido");
            problem.setDetail("El identificador de familia provisto no cumple con el formato UUID esperado.");
            problem.setType(URI.create("https://integrityfamily/errors/invalid-family-id"));
            problem.setProperty("code", "INVALID_FAMILY_ID");
            problem.setProperty("correlationId", currentCorrelationId());
            problem.setProperty("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }
        
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Argumento inválido");
        problem.setDetail(ex.getMessage());
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Encabezado requerido ausente");
        problem.setDetail("Falta el encabezado obligatorio: " + ex.getHeaderName());
        problem.setType(URI.create("https://integrityfamily/errors/missing-header"));
        problem.setProperty("code", "MISSING_REQUIRED_HEADER");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(UnsupportedContractVersionException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedContractVersion(
            UnsupportedContractVersionException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_ACCEPTABLE);
        problem.setTitle("Versión de contrato no soportada");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://integrityfamily/errors/family-home-contract-not-supported"));
        problem.setProperty("code", "FAMILY_HOME_CONTRACT_NOT_SUPPORTED");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("[FAMILY-HOME] Excepción no controlada en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Error interno del servidor");
        problem.setDetail("Ha ocurrido un error inesperado al procesar la solicitud.");
        problem.setType(URI.create("https://integrityfamily/errors/internal-error"));
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty("correlationId", currentCorrelationId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private String currentCorrelationId() {
        String id = MDC.get(MDC_KEY);
        return id != null ? id : "N/A";
    }
}
