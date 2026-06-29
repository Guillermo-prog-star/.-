package com.integrityfamily.ecosystem.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.ecosystem.domain.*;
import com.integrityfamily.ecosystem.dto.EcosystemDtos.*;
import com.integrityfamily.ecosystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ecosistema de Apoyo — 5 niveles de red.
 *
 * Principio rector: la familia autoriza cada conexión.
 * Nivel 3 (TERRITORIAL) jamás recibe datos nominales; solo indicadores agregados.
 * Los niveles no se mezclan hacia arriba sin consentimiento adicional explícito.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcosystemService {

    private final FamilyRepository familyRepository;
    private final EcosystemParticipantRepository participantRepository;
    private final FamilyEcosystemLinkRepository linkRepository;
    private final EcosystemAuditService auditService;

    // ── Catálogo de participantes ─────────────────────────────────────────

    @Transactional
    public ParticipantResponse registerParticipant(RegisterParticipantRequest req) {
        if (participantRepository.existsByNameAndNetworkType(req.getName(), req.getNetworkType())) {
            throw new BusinessException(
                    "Ya existe un participante con ese nombre en la red " + req.getNetworkType(),
                    "ECOSYSTEM_CONFLICT", HttpStatus.CONFLICT);
        }
        EcosystemParticipant p = EcosystemParticipant.builder()
                .name(req.getName())
                .networkType(req.getNetworkType())
                .description(req.getDescription())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .website(req.getWebsite())
                .build();
        return toParticipantResponse(participantRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> listParticipants(NetworkType networkType) {
        List<EcosystemParticipant> list = networkType != null
                ? participantRepository.findByNetworkTypeAndActiveTrue(networkType)
                : participantRepository.findByActiveTrue();
        return list.stream().map(this::toParticipantResponse).toList();
    }

    // ── La familia vincula un participante a su ecosistema ────────────────

    @Transactional
    public LinkResponse link(Long familyId, LinkRequest req, String invitedByEmail) {
        getFamily(familyId);

        EcosystemParticipant participant = participantRepository.findById(req.getParticipantId())
                .orElseThrow(() -> new BusinessException(
                        "Participante no encontrado.", "ECOSYSTEM_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!participant.isActive()) {
            throw new BusinessException(
                    "El participante no está disponible.", "ECOSYSTEM_UNPROCESSABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(
                familyId, participant.getId(), EcosystemLinkStatus.REVOKED)) {
            throw new BusinessException(
                    "Este participante ya está vinculado o tiene una invitación pendiente para esta familia.",
                    "ECOSYSTEM_CONFLICT", HttpStatus.CONFLICT);
        }

        EcosystemAccessScopeDto scope = req.getAccessScope() != null
                ? req.getAccessScope() : new EcosystemAccessScopeDto();

        // Nivel territorial: nunca acceso nominal — forzar todo a false
        if (participant.getNetworkType() == NetworkType.TERRITORIAL) {
            scope = new EcosystemAccessScopeDto(); // todos los flags en false
        }

        FamilyEcosystemLink link = FamilyEcosystemLink.builder()
                .familyId(familyId)
                .participant(participant)
                .networkType(participant.getNetworkType())
                .accessLevel(resolveAccessLevel(participant.getNetworkType()))
                .objective(req.getObjective())
                .responsibilities(req.getResponsibilities())
                .validFrom(req.getValidFrom())
                .validUntil(req.getValidUntil())
                .status(EcosystemLinkStatus.INVITED)
                .invitedByEmail(invitedByEmail)
                .invitedAt(LocalDateTime.now())
                .canViewIcfScore(scope.isCanViewIcfScore())
                .canViewRiskLevel(scope.isCanViewRiskLevel())
                .canViewPlanSummary(scope.isCanViewPlanSummary())
                .canViewSprintProgress(scope.isCanViewSprintProgress())
                .canViewCrisisHistory(scope.isCanViewCrisisHistory())
                .canReceiveAlerts(scope.isCanReceiveAlerts())
                .build();

        FamilyEcosystemLink saved = linkRepository.save(link);
        auditService.record(saved, "INVITED", invitedByEmail,
                "Familia vinculó a '" + participant.getName() + "' en red " + participant.getNetworkType());
        log.info("[ECOSYSTEM] Familia {} vinculó a '{}' ({})", familyId, participant.getName(), participant.getNetworkType());
        return toLinkResponse(saved);
    }

    // ── La familia otorga consentimiento ──────────────────────────────────

    @Transactional
    public LinkResponse giveConsent(Long familyId, ConsentRequest req, String consentedByEmail) {
        FamilyEcosystemLink link = getLink(req.getLinkId(), familyId);

        if (link.getStatus() != EcosystemLinkStatus.INVITED) {
            throw new BusinessException(
                    "Solo se puede consentir un vínculo en estado INVITED.",
                    "ECOSYSTEM_UNPROCESSABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (req.getAccessScope() != null) {
            EcosystemAccessScopeDto scope = req.getAccessScope();
            // Nivel territorial nunca recibe acceso nominal
            if (link.getNetworkType() != NetworkType.TERRITORIAL) {
                link.setCanViewIcfScore(scope.isCanViewIcfScore());
                link.setCanViewRiskLevel(scope.isCanViewRiskLevel());
                link.setCanViewPlanSummary(scope.isCanViewPlanSummary());
                link.setCanViewSprintProgress(scope.isCanViewSprintProgress());
                link.setCanViewCrisisHistory(scope.isCanViewCrisisHistory());
            }
            link.setCanReceiveAlerts(scope.isCanReceiveAlerts());
        }

        link.setStatus(EcosystemLinkStatus.ACTIVE);
        link.setConsentedByEmail(consentedByEmail);
        link.setConsentedAt(LocalDateTime.now());

        FamilyEcosystemLink saved = linkRepository.save(link);
        auditService.record(saved, "CONSENT_GRANTED", consentedByEmail,
                "Consentimiento otorgado a '" + link.getParticipant().getName() + "'");
        log.info("[ECOSYSTEM] Familia {} dio consentimiento al vínculo {} ({})",
                familyId, link.getId(), link.getNetworkType());
        return toLinkResponse(saved);
    }

    // ── La familia revoca el acceso ───────────────────────────────────────

    @Transactional
    public LinkResponse revoke(Long familyId, RevokeRequest req, String revokedByEmail) {
        FamilyEcosystemLink link = getLink(req.getLinkId(), familyId);

        if (link.getStatus() == EcosystemLinkStatus.REVOKED) {
            throw new BusinessException(
                    "El vínculo ya fue revocado.", "ECOSYSTEM_UNPROCESSABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        link.setStatus(EcosystemLinkStatus.REVOKED);
        link.setRevokedByEmail(revokedByEmail);
        link.setRevokedAt(LocalDateTime.now());
        link.setRevocationReason(req.getReason());

        FamilyEcosystemLink saved = linkRepository.save(link);
        auditService.record(saved, "REVOKED", revokedByEmail,
                "Acceso revocado" + (req.getReason() != null ? ": " + req.getReason() : ""));
        log.info("[ECOSYSTEM] Familia {} revocó vínculo {} ({})", familyId, link.getId(), link.getNetworkType());
        return toLinkResponse(saved);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FamilyEcosystemSummary getSummary(Long familyId) {
        getFamily(familyId);
        List<FamilyEcosystemLink> all = linkRepository.findByFamilyId(familyId);
        long active = all.stream().filter(l -> l.getStatus() == EcosystemLinkStatus.ACTIVE).count();

        return FamilyEcosystemSummary.builder()
                .familyId(familyId)
                .totalLinks(all.size())
                .activeLinks((int) active)
                .familiar(filterByType(all, NetworkType.FAMILIAR))
                .institutional(filterByType(all, NetworkType.INSTITUTIONAL))
                .community(filterByType(all, NetworkType.COMMUNITY))
                .territorial(filterByType(all, NetworkType.TERRITORIAL))
                .build();
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> getActiveLinks(Long familyId) {
        return linkRepository.findByFamilyIdAndStatus(familyId, EcosystemLinkStatus.ACTIVE)
                .stream().map(this::toLinkResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> getLinksByNetwork(Long familyId, NetworkType networkType) {
        return linkRepository.findByFamilyIdAndNetworkType(familyId, networkType)
                .stream().map(this::toLinkResponse).toList();
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private Family getFamily(Long familyId) {
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException(
                        "Familia no encontrada.", "ECOSYSTEM_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private FamilyEcosystemLink getLink(Long linkId, Long familyId) {
        FamilyEcosystemLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new BusinessException(
                        "Vínculo no encontrado.", "ECOSYSTEM_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!link.getFamilyId().equals(familyId)) {
            throw new BusinessException("No autorizado.", "ECOSYSTEM_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        return link;
    }

    private int resolveAccessLevel(NetworkType type) {
        return switch (type) {
            case FAMILIAR      -> 1;
            case PROFESSIONAL  -> 2;
            case INSTITUTIONAL -> 2;
            case COMMUNITY     -> 2;
            case TERRITORIAL   -> 3;
        };
    }

    private List<LinkResponse> filterByType(List<FamilyEcosystemLink> all, NetworkType type) {
        return all.stream()
                .filter(l -> l.getNetworkType() == type)
                .map(this::toLinkResponse)
                .toList();
    }

    private ParticipantResponse toParticipantResponse(EcosystemParticipant p) {
        return ParticipantResponse.builder()
                .id(p.getId()).name(p.getName()).networkType(p.getNetworkType())
                .description(p.getDescription()).contactEmail(p.getContactEmail())
                .contactPhone(p.getContactPhone()).website(p.getWebsite())
                .active(p.isActive())
                .build();
    }

    private LinkResponse toLinkResponse(FamilyEcosystemLink l) {
        EcosystemAccessScopeDto scope = new EcosystemAccessScopeDto();
        scope.setCanViewIcfScore(l.isCanViewIcfScore());
        scope.setCanViewRiskLevel(l.isCanViewRiskLevel());
        scope.setCanViewPlanSummary(l.isCanViewPlanSummary());
        scope.setCanViewSprintProgress(l.isCanViewSprintProgress());
        scope.setCanViewCrisisHistory(l.isCanViewCrisisHistory());
        scope.setCanReceiveAlerts(l.isCanReceiveAlerts());

        return LinkResponse.builder()
                .id(l.getId()).familyId(l.getFamilyId())
                .participant(toParticipantResponse(l.getParticipant()))
                .networkType(l.getNetworkType())
                .accessLevel(l.getAccessLevel())
                .objective(l.getObjective())
                .responsibilities(l.getResponsibilities())
                .validFrom(l.getValidFrom()).validUntil(l.getValidUntil())
                .expired(l.isExpired())
                .status(l.getStatus())
                .invitedByEmail(l.getInvitedByEmail()).invitedAt(l.getInvitedAt())
                .consentedByEmail(l.getConsentedByEmail()).consentedAt(l.getConsentedAt())
                .accessScope(scope)
                .build();
    }
}
