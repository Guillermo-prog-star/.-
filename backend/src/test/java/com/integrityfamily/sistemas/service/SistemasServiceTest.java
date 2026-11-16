package com.integrityfamily.sistemas.service;

import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.SprintDaily;
import com.integrityfamily.domain.SprintRetrospective;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.SprintDailyRepository;
import com.integrityfamily.domain.repository.SprintRetrospectiveRepository;
import com.integrityfamily.sistemas.domain.IfSystemCheck;
import com.integrityfamily.sistemas.domain.IfSystemReview;
import com.integrityfamily.sistemas.domain.SystemReviewType;
import com.integrityfamily.sistemas.dto.SistemasDtos.AdjustmentInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.FourWeekCheckResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.ObjectiveInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ProjectInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ReviewResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveCheckRequest;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveReviewRequest;
import com.integrityfamily.sistemas.repository.IfSystemCheckRepository;
import com.integrityfamily.sistemas.repository.IfSystemReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SistemasService — Unit Tests")
class SistemasServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final String EMAIL = "ana@familia.test";

    @Mock MemberRepository memberRepository;
    @Mock IfSystemReviewRepository reviewRepository;
    @Mock IfSystemCheckRepository checkRepository;
    @Mock FamilySprintRepository sprintRepository;
    @Mock SprintDailyRepository dailyRepository;
    @Mock SprintRetrospectiveRepository retrospectiveRepository;
    @Mock AuditService auditService;
    @Mock Clock clock;

    @InjectMocks SistemasService service;

    private Family family;
    private FamilyMember author;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota"));
        lenient().when(clock.instant()).thenReturn(fixed.instant());
        lenient().when(clock.getZone()).thenReturn(fixed.getZone());

        family = Family.builder().id(1L).name("Familia López").build();
        author = FamilyMember.builder()
                .id(10L).family(family).email(EMAIL).fullName("Ana López").firstName("Ana").active(true)
                .build();
    }

    @Nested
    @DisplayName("saveReview()")
    class SaveReview {

        @Test
        @DisplayName("la diaria se liga al check-in del sprint cuando ya existe a nombre de quien revisa")
        void linksExistingDailyCheckin() {
            FamilySprint sprint = FamilySprint.builder().id(4L).family(family).status("ACTIVE").build();
            SprintDaily daily = SprintDaily.builder().id(8L).memberName("ana lópez").checkinDate(TODAY).build();
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.of(sprint));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(4L)).thenReturn(List.of(daily));
            when(reviewRepository.save(any())).thenAnswer(invocation -> {
                IfSystemReview review = invocation.getArgument(0);
                review.setId(50L);
                return review;
            });

            ReviewResponse response = service.saveReview(1L, EMAIL, dailyRequest(List.of("Llamar", "Regar")));

            assertThat(response.sprintId()).isEqualTo(4L);
            assertThat(response.sprintDailyId()).isEqualTo(8L);
            assertThat(response.actions()).containsExactly("Llamar", "Regar");
            assertThat(response.rhythmNote()).contains("ligada al check-in");
            verify(dailyRepository, never()).save(any());
            verify(auditService).registerSystemEvent(eq(EMAIL), eq(AuditEventType.SISTEMAS_REVIEW_RECORDED), anyString());
        }

        @Test
        @DisplayName("si hay sprint pero no check-in de esa persona, guarda la revisión y no fabrica el check-in")
        void doesNotCreateTheCheckin() {
            FamilySprint sprint = FamilySprint.builder().id(4L).family(family).status("ACTIVE").build();
            SprintDaily other = SprintDaily.builder().id(9L).memberName("Luis").checkinDate(TODAY).build();
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.of(sprint));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(4L)).thenReturn(List.of(other));
            when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponse response = service.saveReview(1L, EMAIL, dailyRequest(List.of("Una")));

            assertThat(response.sprintId()).isEqualTo(4L);
            assertThat(response.sprintDailyId()).isNull();
            assertThat(response.rhythmNote()).contains("no tiene check-in");
            verify(dailyRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin sprint activo la diaria se guarda igual")
        void dailyWithoutSprint() {
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.empty());
            when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponse response = service.saveReview(1L, EMAIL, dailyRequest(List.of("Una")));

            assertThat(response.sprintId()).isNull();
            assertThat(response.rhythmNote()).contains("No hay un sprint activo");
        }

        @Test
        @DisplayName("la semanal se liga a la retrospectiva existente y no cierra el sprint ni llama a la IA")
        void linksExistingRetrospectiveWithoutClosing() {
            FamilySprint closed = FamilySprint.builder()
                    .id(6L).family(family).status("COMPLETED").endDate(TODAY.minusDays(1)).build();
            SprintRetrospective retro = SprintRetrospective.builder().id(12L).sprint(closed).whatToAdjust("Ajustar horarios").build();
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(closed));
            when(retrospectiveRepository.findBySprintId(6L)).thenReturn(Optional.of(retro));
            when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponse response = service.saveReview(1L, EMAIL, weeklyRequest(List.of("Cena sin pantallas")));

            assertThat(response.retrospectiveId()).isEqualTo(12L);
            assertThat(response.sprintId()).isEqualTo(6L);
            assertThat(response.priorities()).containsExactly("Cena sin pantallas");
            assertThat(response.rhythmNote()).contains("no se cerró el sprint");
            verify(sprintRepository, never()).save(any());
            verify(retrospectiveRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin retrospectiva, la semanal deja las prioridades y no escribe whatToAdjust")
        void weeklyWithoutRetrospective() {
            FamilySprint active = FamilySprint.builder()
                    .id(4L).family(family).status("ACTIVE").endDate(TODAY.plusDays(5)).build();
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(active));
            when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponse response = service.saveReview(1L, EMAIL, weeklyRequest(List.of("Una", "Dos", "Tres")));

            assertThat(response.retrospectiveId()).isNull();
            assertThat(response.sprintId()).isEqualTo(4L);
            assertThat(response.priorities()).hasSize(3);
            assertThat(response.rhythmNote()).contains("no lo cierra");
            verify(retrospectiveRepository, never()).findBySprintId(any());
            verify(retrospectiveRepository, never()).save(any());
        }

        @Test
        @DisplayName("mensual, anual y ajuste guardan la salida mínima sin tocar el sprint")
        void otherReviewsDoNotTouchTheSprint() {
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponse monthly = service.saveReview(1L, EMAIL, new SaveReviewRequest(
                    "MONTHLY", TODAY, null, null, null, null, null, null, null, null,
                    List.of(new ProjectInput("Huerto", "PAUSAR")), false, null, null, null, null));
            ReviewResponse annual = service.saveReview(1L, EMAIL, new SaveReviewRequest(
                    "ANNUAL", TODAY, null, null, null, null, null, null, null, null, null, null,
                    List.of(new ObjectiveInput("Caminar juntos", "Dos caminatas por semana", LocalDate.of(2027, 6, 1))),
                    null, null, null));
            ReviewResponse change = service.saveReview(1L, EMAIL, new SaveReviewRequest(
                    "CHANGE", TODAY, null, null, null, null, null, null, null, null, null, null, null,
                    "Colegio nuevo", "El entrenamiento del martes",
                    List.of(new AdjustmentInput("Entrenamiento del martes", "REDUCIR"))));

            assertThat(monthly.projects()).singleElement().satisfies(project -> {
                assertThat(project.decision()).isEqualTo("PAUSAR");
                assertThat(project.decisionLabel()).isEqualTo("Pausar");
            });
            assertThat(monthly.stageChangeUnnoticed()).isFalse();
            assertThat(annual.objectives()).singleElement().satisfies(objective -> {
                assertThat(objective.achievementCriterion()).isEqualTo("Dos caminatas por semana");
                assertThat(objective.reviewOn()).isEqualTo(LocalDate.of(2027, 6, 1));
            });
            assertThat(change.whatNoLongerFits()).contains("martes");
            assertThat(change.adjustments()).singleElement().satisfies(adjustment ->
                    assertThat(adjustment.kind()).isEqualTo("REDUCIR"));
            verify(sprintRepository, never()).save(any());
        }

        @Test
        @DisplayName("el servidor rechaza una diaria con cuatro acciones")
        void enforcesDailyLimit() {
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));

            assertThatThrownBy(() -> service.saveReview(1L, EMAIL, dailyRequest(List.of("1", "2", "3", "4"))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión diaria deja de 1 a 3 acciones para hoy.");
            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("quien no es miembro activo de la familia no guarda")
        void rejectsAuthorOutsideTheFamily() {
            Family other = Family.builder().id(99L).name("Otra").build();
            FamilyMember outsider = FamilyMember.builder().id(3L).family(other).email(EMAIL).active(true).build();
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(outsider));

            assertThatThrownBy(() -> service.saveReview(1L, EMAIL, dailyRequest(List.of("Una"))))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "MEMBER_REQUIRED");

            author.setActive(false);
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            assertThatThrownBy(() -> service.saveReview(1L, EMAIL, dailyRequest(List.of("Una"))))
                    .isInstanceOf(BusinessException.class);

            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.saveReview(1L, EMAIL, dailyRequest(List.of("Una"))))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("fourWeekCheck()")
    class FourWeekCheck {

        @Test
        @DisplayName("cuenta semanas con semanal y captura vaciada, e ignora lo anterior a la ventana")
        void countsWeeksInsideTheWindow() {
            when(reviewRepository.findInWindow(eq(1L), eq(LocalDate.of(2026, 10, 20)), eq(TODAY)))
                    .thenReturn(List.of(
                            review(SystemReviewType.WEEKLY, LocalDate.of(2026, 10, 19), null),
                            review(SystemReviewType.WEEKLY, LocalDate.of(2026, 10, 20), null),
                            review(SystemReviewType.WEEKLY, LocalDate.of(2026, 11, 3), null),
                            review(SystemReviewType.WEEKLY, LocalDate.of(2026, 11, 10), null),
                            review(SystemReviewType.DAILY, LocalDate.of(2026, 10, 21), true),
                            review(SystemReviewType.DAILY, LocalDate.of(2026, 10, 28), true),
                            review(SystemReviewType.DAILY, LocalDate.of(2026, 11, 4), true),
                            review(SystemReviewType.DAILY, LocalDate.of(2026, 11, 12), true)
                    ));
            when(checkRepository.findTop12ByFamily_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            FourWeekCheckResponse check = service.fourWeekCheck(1L);

            assertThat(check.windowStart()).isEqualTo(LocalDate.of(2026, 10, 20));
            assertThat(check.weeksWithWeeklyReview()).isEqualTo(3);
            assertThat(check.weeklyReviewsMet()).isTrue();
            assertThat(check.captureEmptiedEveryWeek()).isTrue();
            assertThat(check.weeks()).hasSize(4);
            assertThat(check.unregisteredCommitment()).isNull();
        }

        @Test
        @DisplayName("dos semanales, o una semana sin vaciar la captura, no cumplen la prueba")
        void missesTheBar() {
            when(reviewRepository.findInWindow(any(), any(), any())).thenReturn(List.of(
                    review(SystemReviewType.WEEKLY, LocalDate.of(2026, 11, 10), null),
                    review(SystemReviewType.WEEKLY, LocalDate.of(2026, 11, 11), null),
                    review(SystemReviewType.DAILY, LocalDate.of(2026, 10, 21), true)
            ));
            when(checkRepository.findTop12ByFamily_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                    IfSystemCheck.builder()
                            .id(3L).family(family).author(author)
                            .windowStart(LocalDate.of(2026, 10, 20)).windowEnd(TODAY)
                            .weeklyReviewsDone(1).captureEmptiedEveryWeek(false)
                            .unregisteredCommitment(true).build()
            ));

            FourWeekCheckResponse check = service.fourWeekCheck(1L);

            assertThat(check.weeksWithWeeklyReview()).isEqualTo(1);
            assertThat(check.weeklyReviewsMet()).isFalse();
            assertThat(check.captureEmptiedEveryWeek()).isFalse();
            assertThat(check.unregisteredCommitment()).isTrue();
        }
    }

    @Nested
    @DisplayName("saveCheck()")
    class SaveCheck {

        @Test
        @DisplayName("guarda la respuesta que solo la familia puede saber y el recuento de ese momento")
        void storesTheFamilyAnswer() {
            when(memberRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
            when(reviewRepository.findInWindow(any(), any(), any())).thenReturn(List.of());
            List<IfSystemCheck> stored = new ArrayList<>();
            when(checkRepository.save(any())).thenAnswer(invocation -> {
                IfSystemCheck check = invocation.getArgument(0);
                check.setId(9L);
                stored.clear();
                stored.add(check);
                return check;
            });
            when(checkRepository.findTop12ByFamily_IdOrderByCreatedAtDesc(1L)).thenAnswer(invocation -> List.copyOf(stored));

            FourWeekCheckResponse response = service.saveCheck(1L, EMAIL, new SaveCheckRequest(false));

            ArgumentCaptor<IfSystemCheck> captor = ArgumentCaptor.forClass(IfSystemCheck.class);
            verify(checkRepository).save(captor.capture());
            assertThat(captor.getValue().isUnregisteredCommitment()).isFalse();
            assertThat(captor.getValue().getWeeklyReviewsDone()).isZero();
            assertThat(captor.getValue().getWindowEnd()).isEqualTo(TODAY);
            assertThat(response.unregisteredCommitment()).isFalse();
            verify(auditService).registerSystemEvent(eq(EMAIL), eq(AuditEventType.SISTEMAS_CHECK_RECORDED), anyString());
        }

        @Test
        @DisplayName("no guarda la prueba si la familia no responde la pregunta")
        void requiresAnAnswer() {
            assertThatThrownBy(() -> service.saveCheck(1L, EMAIL, new SaveCheckRequest(null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Responde si llegó algún compromiso a la fecha sin estar registrado.");
            verify(checkRepository, never()).save(any());
        }
    }

    private SaveReviewRequest dailyRequest(List<String> actions) {
        return new SaveReviewRequest("DAILY", TODAY, null, true, true, actions,
                null, null, null, null, null, null, null, null, null, null);
    }

    private SaveReviewRequest weeklyRequest(List<String> priorities) {
        return new SaveReviewRequest("WEEKLY", TODAY, null, null, null, null,
                true, true, true, priorities, null, null, null, null, null, null);
    }

    private IfSystemReview review(SystemReviewType type, LocalDate date, Boolean capture) {
        return IfSystemReview.builder().reviewType(type).reviewDate(date).captureEmptied(capture).build();
    }
}
