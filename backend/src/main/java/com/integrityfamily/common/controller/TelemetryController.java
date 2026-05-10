package com.integrityfamily.common.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.auth.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * TelemetryController: Registra telemetría inteligente del sistema (CLI, evaluaciones, hábitos).
 * Postura Técnica: Delegación directa a AuditService respetando límites de seguridad y auditoría.
 */
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Slf4j
public class TelemetryController {

    private final AuditService auditService;

    @PostMapping
    public ApiResponse<String> logEvent(
            @RequestBody TelemetryRequest request,
            Principal principal,
            HttpServletRequest httpServletRequest) {

        String email = principal != null ? principal.getName() : "ANONYMOUS";
        
        try {
            AuditEventType type = AuditEventType.valueOf(request.eventType());
            auditService.register(email, type, httpServletRequest, request.metadataJson());
            log.info("[TELEMETRY] Event logged successfully: {} by {}", type, email);
            return ApiResponse.ok("Telemetry registered");
        } catch (IllegalArgumentException e) {
            log.warn("[TELEMETRY] Invalid event type: {}", request.eventType());
            return ApiResponse.error("Invalid event type: " + request.eventType());
        } catch (Exception e) {
            log.error("[TELEMETRY] Failed to register telemetry: {}", e.getMessage(), e);
            return ApiResponse.error("Internal telemetry error");
        }
    }

    public record TelemetryRequest(String eventType, String metadataJson) {}
}
