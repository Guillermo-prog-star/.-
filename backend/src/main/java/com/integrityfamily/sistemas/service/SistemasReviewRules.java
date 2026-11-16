package com.integrityfamily.sistemas.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.sistemas.domain.AdjustmentKind;
import com.integrityfamily.sistemas.domain.ProjectDecision;
import com.integrityfamily.sistemas.domain.ReviewItemKind;
import com.integrityfamily.sistemas.domain.SystemReviewType;
import com.integrityfamily.sistemas.dto.SistemasDtos.AdjustmentInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ObjectiveInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ProjectInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveReviewRequest;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Límites de la salida escrita. No persiste nada: el servicio guarda solo lo que esto acepta.
 */
public final class SistemasReviewRules {

    public static final int DAILY_ACTIONS_MIN = 1;
    public static final int DAILY_ACTIONS_MAX = 3;
    public static final int WEEKLY_PRIORITIES_MIN = 1;
    public static final int WEEKLY_PRIORITIES_MAX = 3;
    public static final int MONTHLY_PROJECTS_MIN = 1;
    public static final int MONTHLY_PROJECTS_MAX = 20;
    public static final int ANNUAL_OBJECTIVES_MIN = 1;
    public static final int ANNUAL_OBJECTIVES_MAX = 5;
    public static final int CHANGE_ADJUSTMENTS_MIN = 1;
    public static final int CHANGE_ADJUSTMENTS_MAX = 12;
    public static final int SHORT_TEXT = 280;
    public static final int MEDIUM_TEXT = 500;
    public static final int LONG_TEXT = 2000;
    public static final int LABEL_MAX = 120;
    public static final int CHANGE_DESCRIPTION_MAX = 240;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es"));

    private SistemasReviewRules() {}

    public record ItemDraft(
            ReviewItemKind kind,
            String text,
            String decision,
            String achievementCriterion,
            LocalDate reviewOn
    ) {}

    public record ValidatedReview(
            SystemReviewType type,
            LocalDate reviewDate,
            String periodLabel,
            Boolean captureEmptied,
            Boolean agendaChecked,
            Boolean calendarReviewed,
            Boolean tasksPruned,
            Boolean commitmentsTransferred,
            Boolean stageChangeUnnoticed,
            String changeDescription,
            String whatNoLongerFits,
            List<ItemDraft> items
    ) {}

    public static ValidatedReview validate(SaveReviewRequest request, LocalDate today) {
        if (request == null) {
            throw bad("REVIEW_BODY_REQUIRED", "La revisión no trae datos.");
        }
        SystemReviewType type = SystemReviewType.from(request.type());
        if (type == null) {
            throw bad("REVIEW_TYPE_INVALID", "El tipo de revisión no es válido.");
        }
        if (request.reviewDate() == null) {
            throw bad("REVIEW_DATE_REQUIRED", "La fecha de la revisión es obligatoria.");
        }
        if (today != null && request.reviewDate().isAfter(today)) {
            throw bad("REVIEW_DATE_IN_FUTURE", "La revisión no puede quedar en una fecha futura.");
        }
        String period = blankToNull(request.periodLabel());
        if (period != null && period.length() > LABEL_MAX) {
            throw bad("PERIOD_LABEL_TOO_LONG", "El título del periodo es demasiado largo.");
        }

        return switch (type) {
            case DAILY -> daily(request, period);
            case WEEKLY -> weekly(request, period);
            case MONTHLY -> monthly(request, period);
            case ANNUAL -> annual(request, period);
            case CHANGE -> change(request, period);
        };
    }

    private static ValidatedReview daily(SaveReviewRequest request, String period) {
        if (!Boolean.TRUE.equals(request.captureEmptied())) {
            throw bad("CAPTURE_NOT_EMPTIED", "La revisión diaria exige dejar la captura vacía.");
        }
        if (!Boolean.TRUE.equals(request.agendaChecked())) {
            throw bad("AGENDA_NOT_CHECKED", "La revisión diaria exige comprobar la agenda de hoy.");
        }
        List<String> actions = cleanLines(request.actions());
        if (actions.size() < DAILY_ACTIONS_MIN || actions.size() > DAILY_ACTIONS_MAX) {
            throw bad("DAILY_ACTIONS_COUNT", "La revisión diaria deja de 1 a 3 acciones para hoy.");
        }
        actions.forEach(action -> requireLength(action, SHORT_TEXT));
        return base(SystemReviewType.DAILY, request, period != null ? period : "Día " + request.reviewDate())
                .capture(true)
                .agenda(true)
                .items(lines(ReviewItemKind.ACTION, actions))
                .build();
    }

    private static ValidatedReview weekly(SaveReviewRequest request, String period) {
        if (!Boolean.TRUE.equals(request.calendarReviewed())) {
            throw bad("WEEKLY_CALENDAR_NOT_REVIEWED", "La revisión semanal exige marcar el calendario como revisado.");
        }
        if (!Boolean.TRUE.equals(request.tasksPruned())) {
            throw bad("WEEKLY_TASKS_NOT_PRUNED", "La revisión semanal exige depurar las tareas.");
        }
        if (!Boolean.TRUE.equals(request.commitmentsTransferred())) {
            throw bad("WEEKLY_COMMITMENTS_NOT_TRANSFERRED",
                    "La revisión semanal exige pasar los compromisos de correo o chat a tarea o evento.");
        }
        List<String> priorities = cleanLines(request.priorities());
        if (priorities.size() < WEEKLY_PRIORITIES_MIN || priorities.size() > WEEKLY_PRIORITIES_MAX) {
            throw bad("WEEKLY_PRIORITIES_COUNT", "La revisión semanal deja como máximo 3 prioridades, y al menos una.");
        }
        priorities.forEach(priority -> requireLength(priority, SHORT_TEXT));
        String label = period != null ? period : "Semana del " + request.reviewDate();
        return base(SystemReviewType.WEEKLY, request, label)
                .calendar(true)
                .tasks(true)
                .commitments(true)
                .items(lines(ReviewItemKind.PRIORITY, priorities))
                .build();
    }

    private static ValidatedReview monthly(SaveReviewRequest request, String period) {
        if (request.stageChangeUnnoticed() == null) {
            throw bad("MONTHLY_STAGE_UNANSWERED", "Responde si algún cambio de etapa pasó desapercibido.");
        }
        List<ProjectInput> raw = request.projects() == null ? List.of() : request.projects();
        List<ItemDraft> projects = new ArrayList<>();
        for (ProjectInput project : raw) {
            if (project == null) {
                continue;
            }
            String name = blankToNull(project.name());
            String decisionRaw = blankToNull(project.decision());
            if (name == null && decisionRaw == null) {
                continue;
            }
            if (name == null) {
                throw bad("MONTHLY_PROJECT_NAME", "Cada proyecto necesita un nombre.");
            }
            requireLength(name, SHORT_TEXT);
            ProjectDecision decision = ProjectDecision.from(decisionRaw);
            if (decision == null) {
                throw bad(decisionRaw == null ? "MONTHLY_DECISION_REQUIRED" : "MONTHLY_DECISION_INVALID",
                        decisionRaw == null
                                ? "Cada proyecto necesita una decisión: continuar, corregir, pausar o cerrar."
                                : "La decisión del proyecto no es válida. Usa continuar, corregir, pausar o cerrar.");
            }
            projects.add(new ItemDraft(ReviewItemKind.PROJECT, name, decision.name(), null, null));
        }
        if (projects.isEmpty()) {
            throw bad("MONTHLY_PROJECTS_REQUIRED", "La revisión mensual exige al menos un proyecto.");
        }
        if (projects.size() > MONTHLY_PROJECTS_MAX) {
            throw bad("MONTHLY_PROJECTS_COUNT", "Hay demasiados proyectos en esta revisión.");
        }
        String label = period != null ? period : capitalize(request.reviewDate().format(MONTH));
        return base(SystemReviewType.MONTHLY, request, label)
                .stage(request.stageChangeUnnoticed())
                .items(projects)
                .build();
    }

    private static ValidatedReview annual(SaveReviewRequest request, String period) {
        List<ObjectiveInput> raw = request.objectives() == null ? List.of() : request.objectives();
        List<ItemDraft> objectives = new ArrayList<>();
        for (ObjectiveInput objective : raw) {
            if (objective == null) {
                continue;
            }
            String text = blankToNull(objective.objective());
            String criterion = blankToNull(objective.achievementCriterion());
            LocalDate reviewOn = objective.reviewOn();
            if (text == null && criterion == null && reviewOn == null) {
                continue;
            }
            if (text == null || criterion == null || reviewOn == null) {
                throw bad("ANNUAL_OBJECTIVE_INCOMPLETE",
                        "Cada objetivo necesita su texto, su criterio de logro y la fecha en que se revisa.");
            }
            requireLength(text, MEDIUM_TEXT);
            requireLength(criterion, MEDIUM_TEXT);
            objectives.add(new ItemDraft(ReviewItemKind.OBJECTIVE, text, null, criterion, reviewOn));
        }
        if (objectives.size() < ANNUAL_OBJECTIVES_MIN || objectives.size() > ANNUAL_OBJECTIVES_MAX) {
            throw bad("ANNUAL_OBJECTIVES_COUNT", "La revisión anual guarda pocos objetivos: entre 1 y 5.");
        }
        String label = period != null ? period : "Año " + request.reviewDate().getYear();
        return base(SystemReviewType.ANNUAL, request, label).items(objectives).build();
    }

    private static ValidatedReview change(SaveReviewRequest request, String period) {
        String description = blankToNull(request.changeDescription());
        if (description == null) {
            throw bad("CHANGE_DESCRIPTION_REQUIRED", "Describe el cambio que disparó el ajuste.");
        }
        requireLength(description, CHANGE_DESCRIPTION_MAX);
        String noLongerFits = blankToNull(request.whatNoLongerFits());
        if (noLongerFits == null) {
            throw bad("CHANGE_NO_LONGER_FITS_REQUIRED", "Escribe qué compromisos ya no caben.");
        }
        requireLength(noLongerFits, LONG_TEXT);

        List<AdjustmentInput> raw = request.adjustments() == null ? List.of() : request.adjustments();
        List<ItemDraft> adjustments = new ArrayList<>();
        for (AdjustmentInput adjustment : raw) {
            if (adjustment == null) {
                continue;
            }
            String text = blankToNull(adjustment.description());
            String kindRaw = blankToNull(adjustment.kind());
            if (text == null && kindRaw == null) {
                continue;
            }
            AdjustmentKind kind = AdjustmentKind.from(kindRaw);
            if (text == null || kind == null) {
                throw bad("CHANGE_ADJUSTMENT_INVALID",
                        "Cada ajuste debe decir qué cambias y si lo dejas, lo reduces o lo pospones.");
            }
            requireLength(text, SHORT_TEXT);
            adjustments.add(new ItemDraft(ReviewItemKind.ADJUSTMENT, text, kind.name(), null, null));
        }
        if (adjustments.size() < CHANGE_ADJUSTMENTS_MIN) {
            throw bad("CHANGE_ADJUSTMENTS_REQUIRED", "Indica al menos una cosa que dejas, reduces o pospones.");
        }
        if (adjustments.size() > CHANGE_ADJUSTMENTS_MAX) {
            throw bad("CHANGE_ADJUSTMENTS_COUNT", "Hay demasiados ajustes en esta revisión.");
        }
        String label = period != null ? period : clip(description, LABEL_MAX);
        return base(SystemReviewType.CHANGE, request, label)
                .change(description, noLongerFits)
                .items(adjustments)
                .build();
    }

    private static List<String> cleanLines(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : raw) {
            String text = blankToNull(line);
            if (text != null) {
                cleaned.add(text);
            }
        }
        return cleaned;
    }

    private static List<ItemDraft> lines(ReviewItemKind kind, List<String> texts) {
        List<ItemDraft> items = new ArrayList<>();
        for (String text : texts) {
            items.add(new ItemDraft(kind, text, null, null, null));
        }
        return items;
    }

    private static void requireLength(String text, int max) {
        if (text.length() > max) {
            throw bad("TEXT_TOO_LONG", "El texto es demasiado largo.");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static Draft base(SystemReviewType type, SaveReviewRequest request, String period) {
        return new Draft(type, request.reviewDate(), period);
    }

    private static BusinessException bad(String code, String message) {
        return new BusinessException(message, code, HttpStatus.BAD_REQUEST);
    }

    private static final class Draft {
        private final SystemReviewType type;
        private final LocalDate reviewDate;
        private final String periodLabel;
        private Boolean captureEmptied;
        private Boolean agendaChecked;
        private Boolean calendarReviewed;
        private Boolean tasksPruned;
        private Boolean commitmentsTransferred;
        private Boolean stageChangeUnnoticed;
        private String changeDescription;
        private String whatNoLongerFits;
        private List<ItemDraft> items = List.of();

        private Draft(SystemReviewType type, LocalDate reviewDate, String periodLabel) {
            this.type = type;
            this.reviewDate = reviewDate;
            this.periodLabel = periodLabel;
        }

        private Draft capture(boolean value) { this.captureEmptied = value; return this; }
        private Draft agenda(boolean value) { this.agendaChecked = value; return this; }
        private Draft calendar(boolean value) { this.calendarReviewed = value; return this; }
        private Draft tasks(boolean value) { this.tasksPruned = value; return this; }
        private Draft commitments(boolean value) { this.commitmentsTransferred = value; return this; }
        private Draft stage(boolean value) { this.stageChangeUnnoticed = value; return this; }
        private Draft change(String description, String noLongerFits) {
            this.changeDescription = description;
            this.whatNoLongerFits = noLongerFits;
            return this;
        }
        private Draft items(List<ItemDraft> items) { this.items = List.copyOf(items); return this; }

        private ValidatedReview build() {
            return new ValidatedReview(type, reviewDate, periodLabel, captureEmptied, agendaChecked,
                    calendarReviewed, tasksPruned, commitmentsTransferred, stageChangeUnnoticed,
                    changeDescription, whatNoLongerFits, items);
        }
    }
}
