package com.integrityfamily.evaluation.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.dto.TerritorialEvolutionReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service("territorialReportService")
@RequiredArgsConstructor
public class ReportService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;

    @Transactional(readOnly = true)
    public TerritorialEvolutionReportDto getTerritorialReport(Long familyId) {
        com.integrityfamily.domain.repository.FamilySummary family = familyRepository.findProjectedById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        List<Evaluation> evaluations = evaluationRepository.findWithScoresByFamilyId(familyId);

        List<TerritorialEvolutionReportDto.MilestoneReportDto> milestones = evaluations.stream()
                .filter(e -> e.getMilestoneKey() != null)
                .map(e -> {
                    Double emotions = e.getDimensionScores().stream()
                            .filter(ds -> "emociones".equalsIgnoreCase(ds.getDimensionName()))
                            .map(ds -> ds.getScore())
                            .findFirst().orElse(null);
                            
                    Double communication = e.getDimensionScores().stream()
                            .filter(ds -> "comunicacion".equalsIgnoreCase(ds.getDimensionName()))
                            .map(ds -> ds.getScore())
                            .findFirst().orElse(null);
                            
                    Double habits = e.getDimensionScores().stream()
                            .filter(ds -> "habitos".equalsIgnoreCase(ds.getDimensionName()))
                            .map(ds -> ds.getScore())
                            .findFirst().orElse(null);
                            
                    Double time = e.getDimensionScores().stream()
                            .filter(ds -> "tiempos".equalsIgnoreCase(ds.getDimensionName()))
                            .map(ds -> ds.getScore())
                            .findFirst().orElse(null);

                    TerritorialEvolutionReportDto.DimensionScoresDto scores = new TerritorialEvolutionReportDto.DimensionScoresDto(
                            emotions, communication, habits, time
                    );

                    // Checklist summary (simulado por ahora hasta que conectemos ChecklistLog)
                    TerritorialEvolutionReportDto.ChecklistSummaryDto checklist = new TerritorialEvolutionReportDto.ChecklistSummaryDto(
                            10, 8, 1, 80.0, "UP"
                    );

                    return new TerritorialEvolutionReportDto.MilestoneReportDto(
                            e.getMilestoneKey(),
                            0, // Mes (se puede derivar según el hito)
                            e.getFinalizedAt(),
                            e.getIcf(),
                            e.getRiskLevel(),
                            scores,
                            e.getCriticalDimension(),
                            null, // Delta vs prev (se puede calcular ordenando)
                            checklist
                    );
                })
                .collect(Collectors.toList());

        return new TerritorialEvolutionReportDto(
                family.getFamilyCode(),
                family.getId(),
                family.getName(),
                family.getCountryCode(), family.getDepartmentCode(), null, family.getMunicipio(), null, null, null, null,
                milestones,
                "UP", "ESTABLE", java.util.Collections.emptyList(), "Reporte generado"
        );
    }
}
