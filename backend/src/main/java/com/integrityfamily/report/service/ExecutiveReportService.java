package com.integrityfamily.report.service;

import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.ChecklistRepository;
import com.integrityfamily.report.dto.TransformationSummary;
import com.integrityfamily.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * SDD-REP-01: Executive Report Service.
 * Consolidates historical data to feed the AI Narrative Engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutiveReportService {

    private final FamilyRepository familyRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final ChecklistRepository checklistRepository;
    private final NotificationLogRepository notificationLogRepository;

    /**
     * Aggregates raw historical data into a structured summary.
     */
    @Transactional(readOnly = true)
    public TransformationSummary generateRawSummary(Long familyId) {
        log.info("Ã°Å¸â€œÅ  [REPORT-ENGINE] Consolidating historical data for family {}", familyId);

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));

        List<RiskSnapshot> riskHistory = riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        
        Double initialIcf = riskHistory.isEmpty() ? 0.0 : 
                riskHistory.get(riskHistory.size() - 1).getIcf();
        
        Double currentIcf = riskHistory.isEmpty() ? 0.0 : 
                riskHistory.get(0).getIcf();
        
        Double peakIcf = riskHistory.stream()
                .map(RiskSnapshot::getIcf)
                .max(Comparator.naturalOrder())
                .orElse(0.0);

        long sentinelAlerts = notificationLogRepository.countByFamilyIdAndType(familyId, "CRISIS_ALERT");
        
        long missionsCompleted = checklistRepository.countByFamilyIdAndCompletedTrue(familyId);

        // SDD-REP-01.1: Benchmarking Regional
        Double regionalAvg = calculateRegionalAverage(family.getMunicipio());

        return new TransformationSummary(
                family.getId(),
                family.getName(),
                initialIcf,
                currentIcf,
                peakIcf,
                regionalAvg,
                sentinelAlerts,
                missionsCompleted,
                family.getCurrentMilestone(),
                Collections.emptyList()
        );
    }

    private Double calculateRegionalAverage(String municipio) {
        if (municipio == null) return 50.0; // Default de plataforma
        
        List<Family> families = familyRepository.findByMunicipio(municipio);
        
        double avg = families.stream()
                .map(f -> riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(f.getId()))
                .filter(java.util.Optional::isPresent)
                .mapToDouble(opt -> opt.get().getIcf())
                .average()
                .orElse(50.0);
        
        log.info("📊 [BENCHMARK] Calculando promedio regional para: {}. Resultado: {}", municipio, avg);
        return avg;
    }
}


