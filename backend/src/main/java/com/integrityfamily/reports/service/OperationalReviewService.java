package com.integrityfamily.reports.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanAcceptanceStatus;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Revision operativa semanal: familias activas (plan ACCEPTED, ADR-010), misiones
 * y evidencias del periodo, adherencia por periodo (no acumulada -- ver ADR-011
 * y la discusion sobre ConvivenceAnalyticsService.adherence, que es historica
 * y por eso no sirve para comparar contra el periodo anterior) y flags de
 * excepcion calculados en linea (sin motor de revision persistido -- Regla V1.1.1,
 * no se construye ese flujo hasta tener evidencia de que se necesita).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalReviewService {

    private final ImprovementPlanRepository planRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;

    private static final double LOW_ADHERENCE_THRESHOLD = 50.0;
    private static final double ADHERENCE_DROP_THRESHOLD_PP = 20.0;

    @Data
    @Builder
    public static class FamilyReviewRow {
        private Long familyId;
        private String familyCode;
        private String familyName;
        private long missionsPlanned;
        private long missionsCompleted;
        private long missionsOverdue;
        private long evidenceCount;
        private LocalDateTime lastEvidenceAt;
        private Double adherenceCurrent;
        private Double adherencePrevious;
        private Double adherenceDeltaPp;
        private List<String> flags;
    }

    @Data
    @Builder
    public static class MissionRow {
        private Long taskId;
        private Long familyId;
        private String familyCode;
        private String title;
        private String dimension;
        private LocalDateTime dueDate;
        private boolean completed;
        private boolean overdue;
    }

    @Data
    @Builder
    public static class EvidenceRow {
        private Long evidenceId;
        private Long familyId;
        private String familyCode;
        private String evidenceType;
        private String status;
        private LocalDateTime createdAt;
        private String submittedBy;
    }

    @Data
    @Builder
    public static class OperationalReview {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private LocalDate previousPeriodStart;
        private LocalDate previousPeriodEnd;
        private List<FamilyReviewRow> families;
        private List<MissionRow> missions;
        private List<EvidenceRow> evidences;
    }

    @Transactional(readOnly = true)
    public OperationalReview generate(LocalDate periodStart, LocalDate periodEnd) {
        long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        LocalDate previousPeriodEnd = periodStart.minusDays(1);
        LocalDate previousPeriodStart = previousPeriodEnd.minusDays(periodDays - 1);

        LocalDateTime windowStart = periodStart.atStartOfDay();
        LocalDateTime windowEnd = periodEnd.plusDays(1).atStartOfDay();
        LocalDateTime prevWindowStart = previousPeriodStart.atStartOfDay();
        LocalDateTime prevWindowEnd = previousPeriodEnd.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<ImprovementPlan> activePlans = planRepository.findByAcceptanceStatus(PlanAcceptanceStatus.ACCEPTED);
        log.info("📊 [OPERATIONAL-REVIEW] {} familias activas (plan ACCEPTED) para el periodo {} - {}",
                activePlans.size(), periodStart, periodEnd);

        List<FamilyReviewRow> familyRows = new ArrayList<>();
        List<MissionRow> missionRows = new ArrayList<>();
        List<EvidenceRow> evidenceRows = new ArrayList<>();

        for (ImprovementPlan plan : activePlans) {
            Family family = plan.getFamily();
            if (family == null) continue;

            List<PlanTask> currentTasks = plan.getTasks().stream()
                    .filter(t -> inWindow(t.getDueDate(), windowStart, windowEnd))
                    .toList();
            List<PlanTask> previousTasks = plan.getTasks().stream()
                    .filter(t -> inWindow(t.getDueDate(), prevWindowStart, prevWindowEnd))
                    .toList();

            long missionsPlanned = currentTasks.size();
            long missionsCompleted = currentTasks.stream().filter(PlanTask::isCompleted).count();
            long missionsOverdue = currentTasks.stream()
                    .filter(t -> !t.isCompleted() && t.getDueDate() != null && t.getDueDate().isBefore(now))
                    .count();

            Double adherenceCurrent = adherence(currentTasks);
            Double adherencePrevious = adherence(previousTasks);
            Double adherenceDeltaPp = (adherenceCurrent != null && adherencePrevious != null)
                    ? round1(adherenceCurrent - adherencePrevious)
                    : null;

            List<TaskEvidence> familyEvidences = taskEvidenceRepository.findByFamilyId(family.getId());
            List<TaskEvidence> currentEvidences = familyEvidences.stream()
                    .filter(e -> inWindow(e.getCreatedAt(), windowStart, windowEnd))
                    .sorted(Comparator.comparing(TaskEvidence::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            LocalDateTime lastEvidenceAt = currentEvidences.isEmpty() ? null
                    : currentEvidences.get(currentEvidences.size() - 1).getCreatedAt();

            List<String> flags = new ArrayList<>();
            if (missionsPlanned == 0) flags.add("NO_ACTIVE_MISSIONS");
            if (currentEvidences.isEmpty()) flags.add("NO_RECENT_EVIDENCE");
            if (adherenceCurrent != null && adherenceCurrent < LOW_ADHERENCE_THRESHOLD) flags.add("LOW_ADHERENCE");
            if (adherenceDeltaPp != null && adherenceDeltaPp < -ADHERENCE_DROP_THRESHOLD_PP) flags.add("ADHERENCE_DROP");
            if (missionsOverdue > 0) flags.add("OVERDUE_MISSIONS");

            familyRows.add(FamilyReviewRow.builder()
                    .familyId(family.getId())
                    .familyCode(family.getFamilyCode())
                    .familyName(family.getName())
                    .missionsPlanned(missionsPlanned)
                    .missionsCompleted(missionsCompleted)
                    .missionsOverdue(missionsOverdue)
                    .evidenceCount(currentEvidences.size())
                    .lastEvidenceAt(lastEvidenceAt)
                    .adherenceCurrent(adherenceCurrent)
                    .adherencePrevious(adherencePrevious)
                    .adherenceDeltaPp(adherenceDeltaPp)
                    .flags(flags)
                    .build());

            for (PlanTask t : currentTasks) {
                missionRows.add(MissionRow.builder()
                        .taskId(t.getId())
                        .familyId(family.getId())
                        .familyCode(family.getFamilyCode())
                        .title(t.getTitle())
                        .dimension(t.getDimension())
                        .dueDate(t.getDueDate())
                        .completed(t.isCompleted())
                        .overdue(!t.isCompleted() && t.getDueDate() != null && t.getDueDate().isBefore(now))
                        .build());
            }

            for (TaskEvidence e : currentEvidences) {
                evidenceRows.add(EvidenceRow.builder()
                        .evidenceId(e.getId())
                        .familyId(family.getId())
                        .familyCode(family.getFamilyCode())
                        .evidenceType(e.getEvidenceType() != null ? e.getEvidenceType().name() : null)
                        .status(e.getStatus() != null ? e.getStatus().name() : null)
                        .createdAt(e.getCreatedAt())
                        .submittedBy(e.getSubmittedBy())
                        .build());
            }
        }

        return OperationalReview.builder()
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .previousPeriodStart(previousPeriodStart)
                .previousPeriodEnd(previousPeriodEnd)
                .families(familyRows)
                .missions(missionRows)
                .evidences(evidenceRows)
                .build();
    }

    /** Adherencia = tareas completadas / tareas exigibles dentro del periodo dado. Null si no hubo tareas exigibles (no calculable, no 0/100). */
    private Double adherence(List<PlanTask> tasksInWindow) {
        if (tasksInWindow.isEmpty()) return null;
        long completed = tasksInWindow.stream().filter(PlanTask::isCompleted).count();
        return round1((completed * 100.0) / tasksInWindow.size());
    }

    private boolean inWindow(LocalDateTime value, LocalDateTime start, LocalDateTime endExclusive) {
        return value != null && !value.isBefore(start) && value.isBefore(endExclusive);
    }

    private Double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
