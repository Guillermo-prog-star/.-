package com.integrityfamily.ecosystem.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.ecosystem.domain.*;
import com.integrityfamily.ecosystem.dto.EcosystemDataView;
import com.integrityfamily.ecosystem.dto.EcosystemDtos.*;
import com.integrityfamily.ecosystem.repository.FamilyEcosystemLinkRepository;
import com.integrityfamily.family.service.FamilyHealthSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Filtra qué datos ve cada participante del ecosistema según su scope y nivel de acceso.
 *
 * Reglas:
 * - TERRITORIAL (nivel 3): NUNCA datos nominales — solo métricas agregadas anónimas.
 * - Otros niveles: cada campo se entrega solo si el boolean de scope correspondiente es true.
 * - Toda consulta queda registrada en el audit log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcosystemDataViewService {

    private final FamilyEcosystemLinkRepository linkRepository;
    private final FamilyRepository familyRepository;
    private final FamilyHealthSummaryService healthSummaryService;
    private final EcosystemAuditService auditService;

    @Transactional(readOnly = true)
    public EcosystemDataView getDataView(Long familyId, Long linkId, String requestorEmail) {
        FamilyEcosystemLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new BusinessException(
                        "Vínculo no encontrado.", "ECOSYSTEM_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!link.getFamilyId().equals(familyId)) {
            throw new BusinessException("No autorizado.", "ECOSYSTEM_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        if (link.getStatus() != EcosystemLinkStatus.ACTIVE) {
            throw new BusinessException(
                    "El vínculo no está activo. No se puede acceder a datos familiares.",
                    "ECOSYSTEM_FORBIDDEN", HttpStatus.FORBIDDEN);
        }
        if (link.isExpired()) {
            throw new BusinessException(
                    "La vigencia del vínculo ha expirado.",
                    "ECOSYSTEM_FORBIDDEN", HttpStatus.FORBIDDEN);
        }

        // Nivel 3 (TERRITORIAL): bloquear datos nominales sin excepción
        if (link.getNetworkType() == NetworkType.TERRITORIAL) {
            auditService.record(link, "TERRITORIAL_DATA_VIEW", requestorEmail,
                    "Acceso a datos agregados anónimos — nivel territorial");
            return buildTerritorialView(familyId, link);
        }

        // Niveles 1 y 2: datos según scope
        var health = healthSummaryService.summarize(familyId);

        EcosystemDataView.EcosystemDataViewBuilder builder = EcosystemDataView.builder()
                .familyId(familyId)
                .networkType(link.getNetworkType())
                .accessLevel(link.getAccessLevel())
                .participantName(link.getParticipant().getName());

        if (link.isCanViewIcfScore()) {
            builder.icfScore(health.currentIcf())
                    .icfLabel(health.icfLabel())
                    .icfDirection(health.icfDirection());
        }
        if (link.isCanViewRiskLevel()) {
            builder.riskLevel(health.riskLevel())
                    .sentinelActive(health.sentinelActive());
        }
        if (link.isCanViewPlanSummary()) {
            builder.planSummaryAvailable(true);
        }
        if (link.isCanViewSprintProgress()) {
            builder.hasActiveSprint(health.hasActiveSprint())
                    .activeSprintStatus(health.activeSprintStatus());
        }
        if (link.isCanViewCrisisHistory()) {
            builder.crisisHistoryAvailable(true);
        }

        String detail = buildScopeDetail(link);
        auditService.record(link, "DATA_VIEW", requestorEmail, detail);

        return builder.build();
    }

    private EcosystemDataView buildTerritorialView(Long familyId, FamilyEcosystemLink link) {
        // Datos completamente anónimos: la familia nunca es identificable
        var family = familyRepository.findById(familyId).orElseThrow();
        return EcosystemDataView.builder()
                .familyId(null)              // sin ID nominal
                .networkType(NetworkType.TERRITORIAL)
                .accessLevel(3)
                .participantName(link.getParticipant().getName())
                .municipio(family.getMunicipio())
                .departmentCode(family.getDepartmentCode())
                .countryCode(family.getCountryCode())
                .aggregatedOnly(true)
                .build();
    }

    private String buildScopeDetail(FamilyEcosystemLink l) {
        StringBuilder sb = new StringBuilder("Accedió a: ");
        if (l.isCanViewIcfScore())        sb.append("ICF, ");
        if (l.isCanViewRiskLevel())       sb.append("Riesgo, ");
        if (l.isCanViewPlanSummary())     sb.append("Plan, ");
        if (l.isCanViewSprintProgress())  sb.append("Sprint, ");
        if (l.isCanViewCrisisHistory())   sb.append("Historial crisis, ");
        String result = sb.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}
