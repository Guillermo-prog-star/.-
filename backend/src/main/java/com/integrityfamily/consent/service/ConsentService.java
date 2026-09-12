package com.integrityfamily.consent.service;

import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.consent.domain.Consent;
import com.integrityfamily.consent.domain.ConsentStatus;
import com.integrityfamily.consent.dto.ConsentDtos.ConsentResponse;
import com.integrityfamily.consent.dto.ConsentDtos.GrantRequest;
import com.integrityfamily.consent.dto.ConsentDtos.RevokeRequest;
import com.integrityfamily.consent.repository.ConsentRepository;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consentimiento formal (Fase 2 del programa de interoperabilidad con el
 * ecosistema de salud). Principio: la familia decide qué comparte, con
 * quién y hasta cuándo — y puede revocarlo en cualquier momento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    @Transactional
    public ConsentResponse grant(Long familyId, GrantRequest req, String actorEmail) {
        if (!familyRepository.existsById(familyId)) {
            throw new BusinessException("Familia no encontrada.", "CONSENT_FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        if (req.memberId() != null) {
            var member = memberRepository.findById(req.memberId())
                    .orElseThrow(() -> new BusinessException("Miembro no encontrado.", "CONSENT_MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
            if (member.getFamily() == null || !member.getFamily().getId().equals(familyId)) {
                throw new BusinessException("El miembro no pertenece a esta familia.", "CONSENT_FORBIDDEN", HttpStatus.FORBIDDEN);
            }
        }

        Consent consent = Consent.builder()
                .familyId(familyId)
                .memberId(req.memberId())
                .purpose(req.purpose())
                .scope(req.scope())
                .granteeReference(req.granteeReference())
                .status(ConsentStatus.GRANTED)
                .grantedByEmail(actorEmail)
                .grantedAt(LocalDateTime.now())
                .expiresAt(req.expiresAt())
                .build();

        Consent saved = consentRepository.save(consent);

        auditService.registerSystemEvent(actorEmail, AuditEventType.CONSENT_GRANTED,
                "{\"consentId\": " + saved.getId() + ", \"familyId\": " + familyId
                        + ", \"purpose\": \"" + req.purpose() + "\", \"granteeReference\": \"" + req.granteeReference() + "\"}");

        log.info("[CONSENT] Familia {} otorgó consentimiento {} a {} (propósito {})",
                familyId, saved.getId(), req.granteeReference(), req.purpose());

        return toResponse(saved);
    }

    @Transactional
    public ConsentResponse revoke(Long familyId, Long consentId, RevokeRequest req, String actorEmail) {
        Consent consent = getConsent(consentId, familyId);

        if (consent.getStatus() == ConsentStatus.REVOKED) {
            throw new BusinessException("El consentimiento ya fue revocado.", "CONSENT_ALREADY_REVOKED", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        consent.setStatus(ConsentStatus.REVOKED);
        consent.setRevokedByEmail(actorEmail);
        consent.setRevokedAt(LocalDateTime.now());
        consent.setRevocationReason(req != null ? req.reason() : null);

        Consent saved = consentRepository.save(consent);

        auditService.registerSystemEvent(actorEmail, AuditEventType.CONSENT_REVOKED,
                "{\"consentId\": " + saved.getId() + ", \"familyId\": " + familyId + "}");

        log.info("[CONSENT] Familia {} revocó consentimiento {}", familyId, consentId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> list(Long familyId) {
        return consentRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> listActive(Long familyId) {
        return consentRepository.findByFamilyIdAndStatus(familyId, ConsentStatus.GRANTED)
                .stream().filter(Consent::isActive).map(this::toResponse).toList();
    }

    private Consent getConsent(Long consentId, Long familyId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new BusinessException("Consentimiento no encontrado.", "CONSENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!consent.getFamilyId().equals(familyId)) {
            throw new BusinessException("No autorizado.", "CONSENT_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        return consent;
    }

    private ConsentResponse toResponse(Consent c) {
        return new ConsentResponse(
                c.getId(), c.getFamilyId(), c.getMemberId(), c.getPurpose(), c.getScope(),
                c.getGranteeReference(), c.getStatus(), c.getGrantedByEmail(), c.getGrantedAt(),
                c.getExpiresAt(), c.getRevokedByEmail(), c.getRevokedAt(), c.getRevocationReason(),
                c.isActive());
    }
}
