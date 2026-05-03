package com.integrityfamily.auth.service;

import com.integrityfamily.domain.AuditEvent;
import com.integrityfamily.domain.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void register(String userEmail, com.integrityfamily.domain.AuditEventType eventType, HttpServletRequest request, String metadataJson) {

        String ip = extractIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";

        auditEventRepository.save(AuditEvent.builder()
                .actorEmail(userEmail)
                .eventType(eventType)
                .ipAddress(ip)
                .userAgent(userAgent)
                .metadataJson(metadataJson)
                .build());
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        if (request != null) {
             return request.getRemoteAddr();
        }
        return "UNKNOWN";
    }
}


