package com.integrityfamily.analytics.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.EvaluationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;

    @Data
    @Builder
    public static class GlobalStats {
        private long totalFamilies;
        private double averageIcf;
        private long activeSentinels;
        private Map<String, Long> milestoneDistribution;
        private Map<String, Double> dimensionAverages;
    }

    public GlobalStats getAlphaPhaseStats() {
        List<Family> alphaFamilies = familyRepository.findAll().stream()
                .filter(f -> f.getFamilyCode() != null && f.getFamilyCode().startsWith("ALFA-"))
                .collect(Collectors.toList());

        if (alphaFamilies.isEmpty()) {
            return GlobalStats.builder()
                .totalFamilies(0)
                .averageIcf(0.0)
                .activeSentinels(0)
                .milestoneDistribution(new HashMap<>())
                .dimensionAverages(new HashMap<>())
                .build();
        }

        long total = alphaFamilies.size();
        long sentinels = alphaFamilies.stream().filter(f -> f.getSentinelActive() != null && f.getSentinelActive()).count();

        // Promedio ICF
        double globalAvg = alphaFamilies.stream()
                .flatMap(f -> evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                        f.getId(), 
                        EvaluationStatus.FINALIZED
                ).stream())
                .mapToDouble(e -> e.getIcf())
                .average()
                .orElse(0.0);

        // DistribuciÃƒÂ³n de Hitos
        Map<String, Long> milestones = alphaFamilies.stream()
                .collect(Collectors.groupingBy(
                    f -> f.getCurrentMilestone() != null ? f.getCurrentMilestone() : "SIN_HITO",
                    Collectors.counting()
                ));

        // Promedios por DimensiÃƒÂ³n
        Map<String, Double> dimAvgs = new HashMap<>();
        alphaFamilies.forEach(f -> {
            evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                    f.getId(), 
                    EvaluationStatus.FINALIZED
            ).ifPresent(eval -> {
                eval.getDimensionScores().forEach(ds -> {
                    dimAvgs.merge(ds.getDimensionName(), ds.getScore(), Double::sum);
                });
            });
        });

        // Dividir sumas para obtener promedios
        dimAvgs.forEach((k, v) -> dimAvgs.put(k, v / total));

        return GlobalStats.builder()
                .totalFamilies(total)
                .averageIcf(globalAvg)
                .activeSentinels(sentinels)
                .milestoneDistribution(milestones)
                .dimensionAverages(dimAvgs)
                .build();
    }
}


