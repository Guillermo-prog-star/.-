package com.integrityfamily.ecosystem.service;

import com.integrityfamily.ecosystem.domain.EcosystemAccessLog;
import com.integrityfamily.ecosystem.domain.FamilyEcosystemLink;
import com.integrityfamily.ecosystem.dto.EcosystemDtos.AuditLogEntry;
import com.integrityfamily.ecosystem.repository.EcosystemAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Registra y expone el historial de acceso al ecosistema de una familia.
 * Las escrituras son asíncronas para no añadir latencia al flujo principal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcosystemAuditService {

    private final EcosystemAccessLogRepository logRepository;

    @Async
    public void record(FamilyEcosystemLink link, String action, String actorEmail, String detail) {
        try {
            EcosystemAccessLog entry = EcosystemAccessLog.builder()
                    .linkId(link.getId())
                    .familyId(link.getFamilyId())
                    .actorEmail(actorEmail)
                    .action(action)
                    .detail(detail)
                    .accessLevel(link.getAccessLevel())
                    .build();
            logRepository.save(entry);
        } catch (Exception ex) {
            log.warn("[ECOSYSTEM-AUDIT] Error al registrar evento {} para familia {}: {}",
                    action, link.getFamilyId(), ex.getMessage());
        }
    }

    public List<AuditLogEntry> getAuditLog(Long familyId) {
        return logRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(e -> AuditLogEntry.builder()
                        .id(e.getId())
                        .linkId(e.getLinkId())
                        .actorEmail(e.getActorEmail())
                        .action(e.getAction())
                        .detail(e.getDetail())
                        .accessLevel(e.getAccessLevel())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    public List<AuditLogEntry> getAuditLogByLink(Long linkId) {
        return logRepository.findByLinkIdOrderByCreatedAtDesc(linkId)
                .stream()
                .map(e -> AuditLogEntry.builder()
                        .id(e.getId()).linkId(e.getLinkId())
                        .actorEmail(e.getActorEmail()).action(e.getAction())
                        .detail(e.getDetail()).accessLevel(e.getAccessLevel())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }
}
