package com.integrityfamily.risk.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.admin.service.SecurityWatchdogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SDD SPEC: Motor de Inteligencia Predictiva Sentinel.
 */
@Slf4j
@Service
@RequiredArgsConstructor // SDD: InyecciÃƒÂ³n limpia de dependencias
public class RiskService {

    private final RiskSnapshotRepository riskSnapshotRepository;
    private final SecurityWatchdogService watchdogService;

    public List<RiskSnapshot> findAll() {
        return riskSnapshotRepository.findAll();
    }

    public List<RiskSnapshot> findByFamilyId(Long familyId) {
        return riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    public RiskSnapshot findById(Long id) {
        return riskSnapshotRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Instantánea de riesgo no encontrada", "RISK_SNAPSHOT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public RiskSnapshot save(RiskSnapshot snapshot) {
        return riskSnapshotRepository.save(snapshot);
    }

    /**
     * SDD SPEC: CÃƒÂ¡lculo dinÃƒÂ¡mico de riesgo Sentinel.
     */
    @Transactional
    public RiskSnapshot calculateAndCreate(Family family, Double icf, boolean hasCrisis) {
        log.info("Ã°Å¸â€ºÂ¡Ã¯Â¸Â [RISK-ENGINE] Iniciando anÃƒÂ¡lisis dinÃƒÂ¡mico para: {}", family.getName());

        int months = calculateMonthsSinceRegistration(family);
        String riskLevel = calculateDynamicRisk(icf, months, hasCrisis);
        int conLevel = calculateConsciousnessLevel(icf);
        String conLabel = getLabel(conLevel);

        // SDD: OrquestaciÃƒÂ³n del estado de alerta
        if ("CRITICO".equals(riskLevel) || hasCrisis) {
            family.setSentinelActive(true);
            log.warn("Ã°Å¸Å¡Â¨ [SENTINEL] Activado para: {}", family.getName());
            watchdogService.scanForAnomalies();
        } else {
            family.setSentinelActive(false);
        }

        return riskSnapshotRepository.save(RiskSnapshot.builder()
                .family(family)
                .icf(icf)
                .riskLevel(riskLevel)
                .hasCrisis(hasCrisis)
                .consciousnessLevel(conLevel)
                .consciousnessLabel(conLabel)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String calculateDynamicRisk(Double icf, int months, boolean hasCrisis) {
        if (hasCrisis)
            return "CRITICO";

        // Umbrales adaptativos: A mÃƒÂ¡s tiempo en el programa, mayor es la exigencia de
        // ICF
        double thresholdLow = (months <= 6) ? 70.0 : (months <= 18) ? 80.0 : 90.0;
        double thresholdMid = (months <= 6) ? 40.0 : (months <= 18) ? 55.0 : 70.0;

        if (icf >= thresholdLow)
            return "BAJO";
        if (icf >= thresholdMid)
            return "MEDIO";
        return "ALTO";
    }

    private int calculateMonthsSinceRegistration(Family family) {
        String milestone = family.getCurrentMilestone();
        if (milestone == null || milestone.isBlank()) {
            return 0;
        }
        String digitsOnly = milestone.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return 0;
        }
        try {
            long parsed = Long.parseLong(digitsOnly);
            // Si el número es excesivamente grande, limitarlo a un rango razonable (ej. 36 meses max)
            return (int) Math.min(parsed, 36L);
        } catch (NumberFormatException e) {
            log.warn("[RISK-ENGINE] No se pudo parsear el hito como número de meses: '{}'. Usando 0.", milestone);
            return 0;
        }
    }

    private int calculateConsciousnessLevel(Double icf) {
        if (icf < 20)
            return 1;
        if (icf < 40)
            return 2;
        if (icf < 60)
            return 3;
        if (icf < 80)
            return 4;
        return 5;
    }

    private String getLabel(int level) {
        return switch (level) {
            case 1 -> "Inconsciente";
            case 2 -> "Reactiva";
            case 3 -> "Consciente";
            case 4 -> "Madurando";
            case 5 -> "Plena";
            default -> "Indefinido";
        };
    }
}


