package com.integrityfamily.sistemas.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SistemasDtos {

    private SistemasDtos() {}

    public record ScreenLink(String label, String route) {}

    public record SystemView(
            int number,
            String name,
            String functionLabel,
            String functionText,
            String ruleLabel,
            String ruleText,
            String outcomeLabel,
            String outcomeText,
            String whereInIf,
            boolean eventTriggered,
            String reviewPath,
            String duration,
            List<ScreenLink> links
    ) {}

    public record LevelView(int level, String title, String summary, List<SystemView> systems) {}

    public record MonthStep(int week, String focus) {}

    public record CatalogResponse(
            String title,
            String subtitle,
            String problem,
            String failures,
            String counterargument,
            List<LevelView> levels,
            List<MonthStep> firstMonth,
            List<String> checkQuestions,
            String statusNote
    ) {}

    public record ProjectInput(String name, String decision) {}

    public record ObjectiveInput(String objective, String achievementCriterion, LocalDate reviewOn) {}

    public record AdjustmentInput(String description, String kind) {}

    public record SaveReviewRequest(
            String type,
            LocalDate reviewDate,
            String periodLabel,
            Boolean captureEmptied,
            Boolean agendaChecked,
            List<String> actions,
            Boolean calendarReviewed,
            Boolean tasksPruned,
            Boolean commitmentsTransferred,
            List<String> priorities,
            List<ProjectInput> projects,
            Boolean stageChangeUnnoticed,
            List<ObjectiveInput> objectives,
            String changeDescription,
            String whatNoLongerFits,
            List<AdjustmentInput> adjustments
    ) {}

    public record ProjectView(String name, String decision, String decisionLabel) {}

    public record ObjectiveView(String objective, String achievementCriterion, LocalDate reviewOn) {}

    public record AdjustmentView(String description, String kind, String kindLabel) {}

    public record ReviewResponse(
            Long id,
            String type,
            String typeLabel,
            LocalDate reviewDate,
            String periodLabel,
            String authorName,
            Long sprintId,
            Long sprintDailyId,
            Long retrospectiveId,
            String rhythmNote,
            Boolean captureEmptied,
            Boolean agendaChecked,
            List<String> actions,
            Boolean calendarReviewed,
            Boolean tasksPruned,
            Boolean commitmentsTransferred,
            List<String> priorities,
            List<ProjectView> projects,
            Boolean stageChangeUnnoticed,
            List<ObjectiveView> objectives,
            String changeDescription,
            String whatNoLongerFits,
            List<AdjustmentView> adjustments,
            LocalDateTime createdAt
    ) {}

    public record WeekBucketView(
            LocalDate start,
            LocalDate end,
            boolean weeklyReviewDone,
            boolean captureEmptied
    ) {}

    public record SavedCheckView(
            Long id,
            LocalDate windowStart,
            LocalDate windowEnd,
            int weeksWithWeeklyReview,
            boolean captureEmptiedEveryWeek,
            boolean unregisteredCommitment,
            String authorName,
            LocalDateTime createdAt
    ) {}

    public record FourWeekCheckResponse(
            LocalDate windowStart,
            LocalDate windowEnd,
            int weeksWithWeeklyReview,
            int weeksInWindow,
            int weeklyTarget,
            boolean weeklyReviewsMet,
            boolean captureEmptiedEveryWeek,
            List<WeekBucketView> weeks,
            Boolean unregisteredCommitment,
            List<SavedCheckView> savedChecks
    ) {}

    public record SaveCheckRequest(Boolean unregisteredCommitment) {}
}
