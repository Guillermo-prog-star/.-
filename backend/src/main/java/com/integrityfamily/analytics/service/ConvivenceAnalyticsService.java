package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.*;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * SDD Sprint 5: Servicio Analítico Longitudinal de Convivencia Familiar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConvivenceAnalyticsService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlanTaskRepository planTaskRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;
    private final ReflectionRepository reflectionRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;

    @Transactional
    public OperativeDashboardResponse getOperativeDashboard(Long familyId) {
        log.info("📊 [DASHBOARD SPRINT 5] Calculando motor analítico longitudinal para familia ID: {}", familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + familyId, "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        // 1. Diagnóstico Actual
        List<Evaluation> evals = evaluationRepository.findWithScoresByFamilyId(familyId).stream()
                .sorted(Comparator.comparing(Evaluation::getFinalizedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Evaluation latestEval = evals.isEmpty() ? null : evals.get(evals.size() - 1);
        Evaluation firstEval = evals.isEmpty() ? null : evals.get(0);

        double currentDiagnostic = latestEval != null && latestEval.getIcf() != null ? latestEval.getIcf() : 70.0;
        double emotionsScore = getDimensionScore(latestEval, "emociones");
        double communicationScore = getDimensionScore(latestEval, "comunicacion");

        // 2. Adherencia y Cumplimiento
        List<PlanTask> tasks = planTaskRepository.findAll().stream()
                .filter(t -> t.getPlan() != null && t.getPlan().getFamily() != null && t.getPlan().getFamily().getId().equals(familyId))
                .toList();

        long assignedTasks = tasks.size();
        long completedTasks = tasks.stream().filter(PlanTask::isCompleted).count();
        double adherence = assignedTasks > 0 ? ((double) completedTasks / assignedTasks) * 100.0 : 100.0;

        List<TaskEvidence> evidences = taskEvidenceRepository.findAll().stream()
                .filter(e -> e.getFamily() != null && e.getFamily().getId().equals(familyId))
                .toList();
        long validEvidences = evidences.stream().filter(TaskEvidence::isValidated).count();
        double compliance = completedTasks > 0 ? ((double) validEvidences / completedTasks) * 100.0 : adherence;

        // 3. Participación y Evolución
        int activeMembers = family.getMembers() != null && !family.getMembers().isEmpty() ? family.getMembers().size() : 3;
        double participation = 85.0; // MVP base activa

        double evolution = 50.0;
        if (firstEval != null && firstEval.getIcf() != null && latestEval != null && latestEval.getIcf() != null) {
            double diff = latestEval.getIcf() - firstEval.getIcf();
            evolution = Math.min(100.0, Math.max(0.0, 50.0 + diff));
        }

        // 4. Reflexión
        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        double reflectionScore = reflections.isEmpty() ? 70.0 :
                reflections.stream().mapToInt(r -> r.getEmotionalImpact() != null ? r.getEmotionalImpact() * 20 : 60).average().orElse(70.0);

        // 5. Fórmula Oficial de Índice de Convivencia
        double convivenceIndex = (currentDiagnostic * 0.40) + (adherence * 0.20) + (participation * 0.15) + (evolution * 0.15) + (reflectionScore * 0.10);

        String convivenceStatus = convivenceIndex >= 80 ? "Saludable" : convivenceIndex >= 60 ? "Estable" : convivenceIndex >= 40 ? "Vulnerable" : "Crítico";
        String adherenceStatus = adherence >= 80 ? "Alta" : adherence >= 50 ? "Media" : "Baja";

        // 6. Tendencia e Historial
        List<FamilyMetricsSnapshot> history = snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId);
        String trendState = "Estable";
        if (!history.isEmpty()) {
            Double oldIndex = history.get(history.size() - 1).getConvivenceIndex();
            if (oldIndex != null) {
                if (convivenceIndex - oldIndex > 5) trendState = "Mejorando";
                else if (oldIndex - convivenceIndex > 5) trendState = "Deterioro";
            }
        }

        // 7. Alertas Operativas
        List<OperativeAlertDto> alerts = new ArrayList<>();
        if (communicationScore < 25) {
            alerts.add(OperativeAlertDto.builder().alertCode("ALERTA_CRITICAL_COMMUNICATION").severity("CRITICAL").message("Puntaje de comunicación familiar crítico (< 25). Requiere asamblea urgente.").timestamp(LocalDateTime.now().toString()).build());
        }
        if (adherence < 40) {
            alerts.add(OperativeAlertDto.builder().alertCode("ALERTA_LOW_ADHERENCE").severity("WARNING").message("Adherencia al plan por debajo del 40%. Revisar misiones asignadas.").timestamp(LocalDateTime.now().toString()).build());
        }
        boolean inactive = evidences.isEmpty() || evidences.stream().noneMatch(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusDays(14)));
        if (inactive) {
            alerts.add(OperativeAlertDto.builder().alertCode("ALERTA_INACTIVITY").severity("WARNING").message("Abandono detectado: sin evidencias en los últimos 14 días.").timestamp(LocalDateTime.now().toString()).build());
        }
        if (!history.isEmpty() && history.get(0).getConvivenceIndex() != null && history.get(0).getConvivenceIndex() - convivenceIndex > 20) {
            alerts.add(OperativeAlertDto.builder().alertCode("ALERTA_REGRESSION").severity("CRITICAL").message("Deterioro longitudinal grave: caída de más de 20 puntos en el índice.").timestamp(LocalDateTime.now().toString()).build());
        }

        // 8. Snapshot
        FamilyMetricsSnapshot snapshot = FamilyMetricsSnapshot.builder()
                .familyId(familyId)
                .snapshotDate(LocalDate.now())
                .convivenceIndex(convivenceIndex)
                .riskLevel(latestEval != null && latestEval.getRiskLevel() != null ? latestEval.getRiskLevel() : "MODERATE")
                .adherence(adherence)
                .participation(participation)
                .emotionsScore(emotionsScore)
                .communicationScore(communicationScore)
                .createdAt(LocalDateTime.now())
                .build();
        snapshotRepository.save(snapshot);

        // 9. Timeline y Participación
        List<MetricsTimelineDto> timeline = history.stream().map(h -> MetricsTimelineDto.builder()
                .date(h.getSnapshotDate())
                .convivenceIndex(h.getConvivenceIndex())
                .riskScore(h.getEmotionsScore())
                .adherenceRate(h.getAdherence())
                .build()).toList();

        List<MemberParticipationDto> partList = new ArrayList<>();
        if (family.getMembers() != null) {
            for (FamilyMember m : family.getMembers()) {
                partList.add(MemberParticipationDto.builder()
                        .memberId(m.getId())
                        .memberName(m.getFullName())
                        .role(m.getRole())
                        .activitiesParticipated(5)
                        .participationPercentage(80.0)
                        .build());
            }
        }

        return OperativeDashboardResponse.builder()
                .familyId(familyId)
                .convivenceIndex(convivenceIndex)
                .convivenceStatus(convivenceStatus)
                .currentRiskLevel(latestEval != null && latestEval.getRiskLevel() != null ? latestEval.getRiskLevel() : "MODERATE")
                .trendState(trendState)
                .adherenceRate(adherence)
                .adherenceStatus(adherenceStatus)
                .complianceRate(compliance)
                .emotionsScore(emotionsScore)
                .communicationScore(communicationScore)
                .rachaActivaDias(15)
                .activeAlerts(alerts)
                .timeline(timeline)
                .participation(partList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MetricsTimelineDto> getMetricsTimeline(Long familyId) {
        return snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId).stream()
                .map(h -> MetricsTimelineDto.builder()
                        .date(h.getSnapshotDate())
                        .convivenceIndex(h.getConvivenceIndex())
                        .riskScore(h.getEmotionsScore())
                        .adherenceRate(h.getAdherence())
                        .build()).toList();
    }

    @Transactional(readOnly = true)
    public List<OperativeAlertDto> getActiveAlerts(Long familyId) {
        return getOperativeDashboard(familyId).activeAlerts();
    }

    @Transactional(readOnly = true)
    public List<MemberParticipationDto> getMemberParticipation(Long familyId) {
        return getOperativeDashboard(familyId).participation();
    }

    private Double getDimensionScore(Evaluation eval, String key) {
        if (eval == null || eval.getDimensionScores() == null) return 70.0;
        return eval.getDimensionScores().stream()
                .filter(ds -> ds != null && ds.getDimensionName() != null)
                .filter(ds -> ds.getDimensionName().toLowerCase().contains(key))
                .map(ds -> (double) ds.getScore())
                .findFirst()
                .orElse(70.0);
    }
}
