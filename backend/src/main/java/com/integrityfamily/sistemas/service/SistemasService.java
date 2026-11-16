package com.integrityfamily.sistemas.service;

import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.SprintDaily;
import com.integrityfamily.domain.SprintRetrospective;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.SprintDailyRepository;
import com.integrityfamily.domain.repository.SprintRetrospectiveRepository;
import com.integrityfamily.sistemas.domain.AdjustmentKind;
import com.integrityfamily.sistemas.domain.IfSystemCheck;
import com.integrityfamily.sistemas.domain.IfSystemReview;
import com.integrityfamily.sistemas.domain.IfSystemReviewItem;
import com.integrityfamily.sistemas.domain.ProjectDecision;
import com.integrityfamily.sistemas.domain.ReviewItemKind;
import com.integrityfamily.sistemas.domain.SystemReviewType;
import com.integrityfamily.sistemas.dto.SistemasDtos.AdjustmentView;
import com.integrityfamily.sistemas.dto.SistemasDtos.FourWeekCheckResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.ObjectiveView;
import com.integrityfamily.sistemas.dto.SistemasDtos.ProjectView;
import com.integrityfamily.sistemas.dto.SistemasDtos.ReviewResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveCheckRequest;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveReviewRequest;
import com.integrityfamily.sistemas.dto.SistemasDtos.SavedCheckView;
import com.integrityfamily.sistemas.dto.SistemasDtos.WeekBucketView;
import com.integrityfamily.sistemas.repository.IfSystemCheckRepository;
import com.integrityfamily.sistemas.repository.IfSystemReviewRepository;
import com.integrityfamily.sistemas.service.SistemasReviewRules.ItemDraft;
import com.integrityfamily.sistemas.service.SistemasReviewRules.ValidatedReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Capa de práctica de Sistemas IF. Escribe la salida de la revisión y, si ya
 * existen, enlaza el check-in o la retrospectiva. No crea sprints, no los
 * cierra y no llama a la IA: la familia decide el texto (ADR-019, ADR-020).
 */
@Service
public class SistemasService {

    static final int WEEKS_IN_WINDOW = 4;
    static final int WEEKLY_TARGET = 3;
    private static final int WINDOW_DAYS = 27;
    private static final int RETRO_LOOKBACK_DAYS = 15;

    private final MemberRepository memberRepository;
    private final IfSystemReviewRepository reviewRepository;
    private final IfSystemCheckRepository checkRepository;
    private final FamilySprintRepository sprintRepository;
    private final SprintDailyRepository dailyRepository;
    private final SprintRetrospectiveRepository retrospectiveRepository;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public SistemasService(MemberRepository memberRepository,
                           IfSystemReviewRepository reviewRepository,
                           IfSystemCheckRepository checkRepository,
                           FamilySprintRepository sprintRepository,
                           SprintDailyRepository dailyRepository,
                           SprintRetrospectiveRepository retrospectiveRepository,
                           AuditService auditService,
                           @Autowired(required = false) Clock clock) {
        this.memberRepository = memberRepository;
        this.reviewRepository = reviewRepository;
        this.checkRepository = checkRepository;
        this.sprintRepository = sprintRepository;
        this.dailyRepository = dailyRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.auditService = auditService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    @Transactional
    public ReviewResponse saveReview(Long familyId, String authorEmail, SaveReviewRequest request) {
        FamilyMember author = requireAuthor(familyId, authorEmail);
        ValidatedReview validated = SistemasReviewRules.validate(request, today());

        IfSystemReview review = IfSystemReview.builder()
                .family(author.getFamily())
                .author(author)
                .reviewType(validated.type())
                .reviewDate(validated.reviewDate())
                .periodLabel(validated.periodLabel())
                .captureEmptied(validated.captureEmptied())
                .agendaChecked(validated.agendaChecked())
                .calendarReviewed(validated.calendarReviewed())
                .tasksPruned(validated.tasksPruned())
                .commitmentsTransferred(validated.commitmentsTransferred())
                .stageChangeUnnoticed(validated.stageChangeUnnoticed())
                .changeDescription(validated.changeDescription())
                .whatNoLongerFits(validated.whatNoLongerFits())
                .build();

        linkExistingRhythm(review, author, validated);

        int order = 1;
        for (ItemDraft draft : validated.items()) {
            review.addItem(IfSystemReviewItem.builder()
                    .sortOrder(order++)
                    .itemKind(draft.kind())
                    .textValue(draft.text())
                    .decision(draft.decision())
                    .achievementCriterion(draft.achievementCriterion())
                    .reviewOn(draft.reviewOn())
                    .build());
        }

        IfSystemReview saved = reviewRepository.save(review);
        auditService.registerSystemEvent(auditEmail(author, familyId), AuditEventType.SISTEMAS_REVIEW_RECORDED,
                "{\"familyId\":" + familyId + ",\"reviewId\":" + saved.getId()
                        + ",\"type\":\"" + saved.getReviewType().name() + "\"}");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> history(Long familyId) {
        return reviewRepository.findByFamily_IdOrderByReviewDateDescCreatedAtDesc(familyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long familyId, Long reviewId) {
        IfSystemReview review = reviewRepository.findById(reviewId)
                .filter(found -> found.getFamily() != null && familyId.equals(found.getFamily().getId()))
                .orElseThrow(() -> new BusinessException("Revisión no encontrada.", "REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public FourWeekCheckResponse fourWeekCheck(Long familyId) {
        LocalDate end = today();
        LocalDate start = end.minusDays(WINDOW_DAYS);
        List<IfSystemReview> reviews = reviewRepository.findInWindow(familyId, start, end);
        List<WeekBucketView> weeks = buckets(start, reviews);
        int weeksWithWeekly = (int) weeks.stream().filter(WeekBucketView::weeklyReviewDone).count();
        boolean captureEveryWeek = weeks.stream().allMatch(WeekBucketView::captureEmptied);
        List<SavedCheckView> saved = savedChecks(familyId);
        Boolean answer = saved.stream()
                .filter(check -> start.equals(check.windowStart()) && end.equals(check.windowEnd()))
                .map(SavedCheckView::unregisteredCommitment)
                .findFirst()
                .orElse(null);
        return new FourWeekCheckResponse(
                start,
                end,
                weeksWithWeekly,
                WEEKS_IN_WINDOW,
                WEEKLY_TARGET,
                weeksWithWeekly >= WEEKLY_TARGET,
                captureEveryWeek,
                weeks,
                answer,
                saved);
    }

    @Transactional
    public FourWeekCheckResponse saveCheck(Long familyId, String authorEmail, SaveCheckRequest request) {
        if (request == null || request.unregisteredCommitment() == null) {
            throw new BusinessException(
                    "Responde si llegó algún compromiso a la fecha sin estar registrado.",
                    "CHECK_ANSWER_REQUIRED",
                    HttpStatus.BAD_REQUEST);
        }
        FamilyMember author = requireAuthor(familyId, authorEmail);
        FourWeekCheckResponse current = fourWeekCheck(familyId);
        IfSystemCheck saved = checkRepository.save(IfSystemCheck.builder()
                .family(author.getFamily())
                .author(author)
                .windowStart(current.windowStart())
                .windowEnd(current.windowEnd())
                .weeklyReviewsDone(current.weeksWithWeeklyReview())
                .captureEmptiedEveryWeek(current.captureEmptiedEveryWeek())
                .unregisteredCommitment(request.unregisteredCommitment())
                .build());
        auditService.registerSystemEvent(auditEmail(author, familyId), AuditEventType.SISTEMAS_CHECK_RECORDED,
                "{\"familyId\":" + familyId + ",\"checkId\":" + saved.getId() + "}");
        return fourWeekCheck(familyId);
    }

    private void linkExistingRhythm(IfSystemReview review, FamilyMember author, ValidatedReview validated) {
        if (validated.type() == SystemReviewType.DAILY) {
            sprintRepository.findActiveSprintForFamily(review.getFamily().getId()).ifPresent(sprint -> {
                review.setSprint(sprint);
                matchDaily(sprint, author, validated.reviewDate()).ifPresent(review::setSprintDaily);
            });
            return;
        }
        if (validated.type() != SystemReviewType.WEEKLY) {
            return;
        }
        List<FamilySprint> sprints = sprintRepository.findByFamilyIdOrderByCreatedAtDesc(review.getFamily().getId());
        LocalDate from = validated.reviewDate().minusDays(RETRO_LOOKBACK_DAYS);
        LocalDate to = validated.reviewDate().plusDays(1);
        for (FamilySprint sprint : sprints.stream().limit(8).toList()) {
            if (sprint.getEndDate() == null || sprint.getEndDate().isBefore(from) || sprint.getEndDate().isAfter(to)) {
                continue;
            }
            Optional<SprintRetrospective> retro = retrospectiveRepository.findBySprintId(sprint.getId());
            if (retro.isPresent()) {
                review.setSprint(sprint);
                review.setRetrospective(retro.get());
                return;
            }
        }
        sprints.stream()
                .filter(sprint -> "ACTIVE".equals(sprint.getStatus()))
                .findFirst()
                .ifPresent(review::setSprint);
    }

    private Optional<SprintDaily> matchDaily(FamilySprint sprint, FamilyMember author, LocalDate date) {
        return dailyRepository.findBySprintIdOrderByCheckinDateDesc(sprint.getId()).stream()
                .filter(daily -> date.equals(daily.getCheckinDate()))
                .filter(daily -> namesMatch(author, daily.getMemberName()))
                .findFirst();
    }

    static boolean namesMatch(FamilyMember author, String memberName) {
        if (memberName == null || memberName.isBlank()) {
            return false;
        }
        String candidate = memberName.trim();
        return equalsName(candidate, author.getFullName()) || equalsName(candidate, author.getFirstName());
    }

    private static boolean equalsName(String candidate, String known) {
        return known != null && candidate.equalsIgnoreCase(known.trim());
    }

    private List<WeekBucketView> buckets(LocalDate windowStart, List<IfSystemReview> reviews) {
        List<WeekBucketView> weeks = new ArrayList<>();
        for (int index = 0; index < WEEKS_IN_WINDOW; index++) {
            LocalDate start = windowStart.plusDays(index * 7L);
            LocalDate end = start.plusDays(6);
            boolean weekly = false;
            boolean capture = false;
            for (IfSystemReview review : reviews) {
                if (review.getReviewDate() == null) {
                    continue;
                }
                long day = ChronoUnit.DAYS.between(windowStart, review.getReviewDate());
                if (day < 0 || day > WINDOW_DAYS || day / 7 != index) {
                    continue;
                }
                if (review.getReviewType() == SystemReviewType.WEEKLY) {
                    weekly = true;
                }
                if (review.getReviewType() == SystemReviewType.DAILY && Boolean.TRUE.equals(review.getCaptureEmptied())) {
                    capture = true;
                }
            }
            weeks.add(new WeekBucketView(start, end, weekly, capture));
        }
        return weeks;
    }

    private List<SavedCheckView> savedChecks(Long familyId) {
        return checkRepository.findTop12ByFamily_IdOrderByCreatedAtDesc(familyId).stream()
                .map(check -> new SavedCheckView(
                        check.getId(),
                        check.getWindowStart(),
                        check.getWindowEnd(),
                        check.getWeeklyReviewsDone(),
                        check.isCaptureEmptiedEveryWeek(),
                        check.isUnregisteredCommitment(),
                        check.getAuthor() != null ? check.getAuthor().getFullName() : null,
                        check.getCreatedAt()))
                .toList();
    }

    private ReviewResponse toResponse(IfSystemReview review) {
        List<IfSystemReviewItem> items = review.getItems() == null
                ? List.of()
                : review.getItems().stream()
                .sorted(Comparator.comparingInt(IfSystemReviewItem::getSortOrder))
                .toList();
        return new ReviewResponse(
                review.getId(),
                review.getReviewType().name(),
                review.getReviewType().label(),
                review.getReviewDate(),
                review.getPeriodLabel(),
                review.getAuthor() != null ? review.getAuthor().getFullName() : null,
                idOf(review.getSprint()),
                review.getSprintDaily() != null ? review.getSprintDaily().getId() : null,
                review.getRetrospective() != null ? review.getRetrospective().getId() : null,
                rhythmNote(review),
                review.getCaptureEmptied(),
                review.getAgendaChecked(),
                texts(items, ReviewItemKind.ACTION),
                review.getCalendarReviewed(),
                review.getTasksPruned(),
                review.getCommitmentsTransferred(),
                texts(items, ReviewItemKind.PRIORITY),
                projects(items),
                review.getStageChangeUnnoticed(),
                objectives(items),
                review.getChangeDescription(),
                review.getWhatNoLongerFits(),
                adjustments(items),
                review.getCreatedAt());
    }

    private static Long idOf(FamilySprint sprint) {
        return sprint != null ? sprint.getId() : null;
    }

    static String rhythmNote(IfSystemReview review) {
        boolean sprint = review.getSprint() != null;
        boolean daily = review.getSprintDaily() != null;
        boolean retro = review.getRetrospective() != null;
        return switch (review.getReviewType()) {
            case DAILY -> {
                if (daily) {
                    yield "Esta revisión queda ligada al check-in del sprint de ese día. No sustituye el registro del sprint.";
                }
                if (sprint) {
                    yield "Hay un sprint activo, pero ese día no tiene check-in de esta persona. Puedes registrarlo en la bitácora. Esta revisión no lo crea sola.";
                }
                yield "No hay un sprint activo. La revisión diaria queda guardada; el check-in del sprint se hace cuando haya uno.";
            }
            case WEEKLY -> retro
                    ? "Esta revisión queda ligada a la retrospectiva del sprint. Las prioridades las escribió la familia; no se cerró el sprint ni intervino la IA."
                    : "Las prioridades quedan escritas aquí. La retrospectiva del sprint se completa al cerrarlo, y esta revisión no lo cierra ni le pide a la IA que decida.";
            case MONTHLY -> "La decisión de cada proyecto queda escrita aquí. Los hitos del plan siguen en el plan familiar.";
            case ANNUAL -> "Cada objetivo queda con su criterio de logro y su fecha de revisión. La visión del plan y los hitos M12 y M24 siguen en el plan.";
            case CHANGE -> "El ajuste queda escrito porque cambió la situación, no porque tocara una fecha. Los hitos y las etapas siguen en la ruta.";
        };
    }

    private static List<String> texts(List<IfSystemReviewItem> items, ReviewItemKind kind) {
        return items.stream()
                .filter(item -> item.getItemKind() == kind)
                .map(IfSystemReviewItem::getTextValue)
                .toList();
    }

    private static List<ProjectView> projects(List<IfSystemReviewItem> items) {
        return items.stream()
                .filter(item -> item.getItemKind() == ReviewItemKind.PROJECT)
                .map(item -> {
                    ProjectDecision decision = ProjectDecision.from(item.getDecision());
                    return new ProjectView(item.getTextValue(), item.getDecision(),
                            decision != null ? decision.label() : item.getDecision());
                })
                .toList();
    }

    private static List<ObjectiveView> objectives(List<IfSystemReviewItem> items) {
        return items.stream()
                .filter(item -> item.getItemKind() == ReviewItemKind.OBJECTIVE)
                .map(item -> new ObjectiveView(item.getTextValue(), item.getAchievementCriterion(), item.getReviewOn()))
                .toList();
    }

    private static List<AdjustmentView> adjustments(List<IfSystemReviewItem> items) {
        return items.stream()
                .filter(item -> item.getItemKind() == ReviewItemKind.ADJUSTMENT)
                .map(item -> {
                    AdjustmentKind kind = AdjustmentKind.from(item.getDecision());
                    return new AdjustmentView(item.getTextValue(), item.getDecision(),
                            kind != null ? kind.label() : item.getDecision());
                })
                .toList();
    }

    private FamilyMember requireAuthor(Long familyId, String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("No autenticado.", "NOT_AUTHENTICATED", HttpStatus.UNAUTHORIZED);
        }
        FamilyMember member = memberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException(
                        "No tienes un perfil de miembro en esta familia.",
                        "MEMBER_REQUIRED",
                        HttpStatus.FORBIDDEN));
        if (!member.isActive() || member.getFamily() == null || !familyId.equals(member.getFamily().getId())) {
            throw new BusinessException(
                    "No tienes un perfil de miembro activo en esta familia.",
                    "MEMBER_REQUIRED",
                    HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private static String auditEmail(FamilyMember author, Long familyId) {
        if (author.getEmail() != null && !author.getEmail().isBlank()) {
            return author.getEmail();
        }
        return "family_" + familyId + "@integrityfamily.com";
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
