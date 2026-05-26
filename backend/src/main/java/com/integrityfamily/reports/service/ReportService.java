package com.integrityfamily.reports.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.RiskLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;

    @Data
    @Builder
    public static class ConsolidatedReport {
        private String reportId;
        private Map<String, Object> metadata;
        private Map<String, DimensionSummary> consolidadoDimensiones;
        private List<CaseRegistry> casosAltoRiesgo;
    }

    @Data
    @Builder
    public static class DimensionSummary {
        private double promedioScore;
        private String nivelAlerta;
    }

    @Data
    @Builder
    public static class CaseRegistry {
        private String familiaId;
        private double puntuacionTotal;
        private String dimensionCritica;
        private String impactoDelta;
    }

    /**
     * Genera un reporte consolidado basado en la metodologÃƒÂ­a SDD.
     * Calcula Deltas de Mejora y Niveles de Riesgo por DimensiÃƒÂ³n.
     */
    public ConsolidatedReport generateConsolidatedReport() {
        log.info("Ã°Å¸â€œÅ  [REPORT-SERVICE] Iniciando generaciÃƒÂ³n de reporte consolidado SDD...");
        
        List<com.integrityfamily.domain.repository.FamilySummary> families = familyRepository.findProjectedBy();
        Map<String, List<Double>> dimensionScoresMap = new HashMap<>();
        List<CaseRegistry> highRiskCases = new ArrayList<>();

        for (com.integrityfamily.domain.repository.FamilySummary family : families) {
            List<Evaluation> evals = evaluationRepository.findWithScoresByFamilyId(family.getId()).stream()
                    .sorted(Comparator.comparing(Evaluation::getFinalizedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (evals.isEmpty()) continue;

            Evaluation preTest = evals.get(0);
            Evaluation postTest = evals.get(evals.size() - 1);

            // Calcular Delta de Mejora General
            double delta = calculateDelta(preTest.getIcf(), postTest.getIcf());
            double currentScorePercent = postTest.getIcf(); // Ya está en escala 0-100%

            // Acumular puntuaciones por dimensión (Mapeo a categorías institucionales)
            postTest.getDimensionScores().forEach(ds -> {
                String mappedDim = mapToInstitutionalDimension(ds.getDimensionName());
                dimensionScoresMap.computeIfAbsent(mappedDim, k -> new ArrayList<>()).add(ds.getScore()); // Ya está en escala 0-100%
            });

            // Detectar Casos Críticos (Algoritmo de Semáforo)
            if (currentScorePercent < 50) {
                String criticalDim = postTest.getDimensionScores().stream()
                        .min(Comparator.comparing(ds -> ds.getScore()))
                        .map(ds -> ds.getDimensionName())
                        .orElse("N/A");

                highRiskCases.add(CaseRegistry.builder()
                        .familiaId(family.getFamilyCode())
                        .puntuacionTotal(currentScorePercent)
                        .dimensionCritica(criticalDim)
                        .impactoDelta(String.format("%+.0f%%", delta))
                        .build());
                
                log.warn("Ã°Å¸Å¡Â¨ [REPORT-CRITICAL] Caso de alto riesgo detectado: {}", family.getFamilyCode());
            }
        }

        // Consolidar promedios de dimensiones
        Map<String, DimensionSummary> dimensionSummaryMap = new HashMap<>();
        dimensionScoresMap.forEach((dim, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            dimensionSummaryMap.put(dim.toLowerCase(), DimensionSummary.builder()
                    .promedioScore(Math.round(avg))
                    .nivelAlerta(RiskLevel.fromScore(avg).getLabel())
                    .build());
        });

        return ConsolidatedReport.builder()
                .reportId("REP-" + System.currentTimeMillis())
                .metadata(Map.of(
                    "total_familias", families.size(),
                    "fecha_generacion", new java.util.Date().toString(),
                    "rango_analisis", "Q1-2026"
                ))
                .consolidadoDimensiones(dimensionSummaryMap)
                .casosAltoRiesgo(highRiskCases)
                .build();
    }

    /**
     * Algoritmo Delta: ÃŽâ€ = ((ScorePost - ScorePre) / ScorePre) * 100
     */
    private double calculateDelta(double pre, double post) {
        if (pre == 0) return 0.0;
        return ((post - pre) / pre) * 100;
    }

    /**
     * Mapeo Transaccional: Convierte pilares pedagÃƒÂ³gicos internos a dimensiones institucionales.
     */
    private String mapToInstitutionalDimension(String internalDim) {
        return switch (internalDim) {
            case "Reconocimiento" -> "ComunicaciÃƒÂ³n";
            case "Amor" -> "Emociones";
            case "Entrega" -> "HÃƒÂ¡bitos / Tiempos";
            default -> internalDim;
        };
    }
}


