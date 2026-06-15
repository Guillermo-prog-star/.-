package com.integrityfamily.movie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.movie.domain.FamilyMovie;
import com.integrityfamily.movie.dto.FamilyMovieDto;
import com.integrityfamily.movie.repository.FamilyMovieRepository;
import com.integrityfamily.ritual.domain.FamilyRitual;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMovieService")
class FamilyMovieServiceTest {

    @Mock FamilyMovieRepository            movieRepository;
    @Mock FamilyRepository                 familyRepository;
    @Mock FamilyGratitudeEntryRepository   gratitudeRepository;
    @Mock TaskEvidenceRepository           evidenceRepository;
    @Mock FamilyLogbookRepository          logbookRepository;
    @Mock CriticalDayRepository            crisisRepository;
    @Mock FamilySprintRepository           sprintRepository;
    @Mock ImprovementPlanRepository        planRepository;
    @Mock FamilyRitualRepository           ritualRepository;
    @Mock AiProvider                       aiProvider;
    @Spy  ObjectMapper                     objectMapper = new ObjectMapper();
    @InjectMocks FamilyMovieService service;

    private static final long    FAM_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO   = LocalDate.of(2026, 3, 31);

    private final Family family = Family.builder().id(FAM_ID).name("García").build();

    private static final String VALID_JSON = """
            {
              "openingLine":    "Un trimestre que cambió todo.",
              "chapter1":       "Se conectaron.",
              "chapter2":       "Superaron.",
              "chapter3":       "Construyeron.",
              "mentorLetter":   "Gracias, familia García.",
              "highlightQuote": "Juntos siempre."
            }
            """;

    /** Stubs mínimos para generate() con todas las fuentes vacías y AI válida. */
    private void stubGenerate(String aiJson) {
        when(movieRepository.existsByFamilyIdAndPeriodStartAndPeriodEnd(FAM_ID, FROM, TO)).thenReturn(false);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
        when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(FAM_ID)).thenReturn(List.of());
        when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
        when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(aiProvider.generateRawResponse(any())).thenReturn(aiJson);
        when(movieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── listMovies ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listMovies")
    class ListMovies {

        @Test
        @DisplayName("sin películas → lista vacía")
        void noMovies_emptyList() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(movieRepository.findByFamilyIdOrderByPeriodStartDesc(FAM_ID)).thenReturn(List.of());

            assertThat(service.listMovies(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("dos películas → dos DTOs con familyName")
        void twoMovies_mappedWithFamilyName() {
            FamilyMovie m1 = FamilyMovie.builder().id(1L).familyId(FAM_ID).periodLabel("Q1").build();
            FamilyMovie m2 = FamilyMovie.builder().id(2L).familyId(FAM_ID).periodLabel("Q2").build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(movieRepository.findByFamilyIdOrderByPeriodStartDesc(FAM_ID)).thenReturn(List.of(m1, m2));

            List<FamilyMovieDto> result = service.listMovies(FAM_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).familyName()).isEqualTo("García");
            assertThat(result.get(1).familyName()).isEqualTo("García");
        }
    }

    // ── getLatest ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLatest")
    class GetLatest {

        @Test
        @DisplayName("sin película → Optional.empty")
        void noMovie_emptyOptional() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(movieRepository.findFirstByFamilyIdOrderByPeriodStartDesc(FAM_ID)).thenReturn(Optional.empty());

            assertThat(service.getLatest(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("con película → Optional con DTO")
        void withMovie_optionalWithDto() {
            FamilyMovie m = FamilyMovie.builder().id(1L).familyId(FAM_ID).periodLabel("Q1").build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(movieRepository.findFirstByFamilyIdOrderByPeriodStartDesc(FAM_ID)).thenReturn(Optional.of(m));

            assertThat(service.getLatest(FAM_ID)).isPresent();
        }
    }

    // ── generate ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate")
    class Generate {

        @Test
        @DisplayName("período ya existe → retorna existente sin llamar a AI")
        void periodAlreadyExists_returnsExistingWithoutAI() {
            FamilyMovie existing = FamilyMovie.builder().id(5L).familyId(FAM_ID)
                    .periodStart(FROM).periodEnd(TO).periodLabel("ene – mar 2026").build();
            when(movieRepository.existsByFamilyIdAndPeriodStartAndPeriodEnd(FAM_ID, FROM, TO)).thenReturn(true);
            when(movieRepository.findByFamilyIdOrderByPeriodStartDesc(FAM_ID)).thenReturn(List.of(existing));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.id()).isEqualTo(5L);
            verifyNoInteractions(aiProvider);
        }

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(movieRepository.existsByFamilyIdAndPeriodStartAndPeriodEnd(FAM_ID, FROM, TO)).thenReturn(false);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());
            lenient().when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            lenient().when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            lenient().when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            lenient().when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            lenient().when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(FAM_ID)).thenReturn(List.of());
            lenient().when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            lenient().when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> service.generate(FAM_ID, FROM, TO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(FAM_ID));
        }

        @Test
        @DisplayName("todas las fuentes vacías → contadores 0, película guardada")
        void allEmpty_savedWithZeroCounters() {
            stubGenerate(VALID_JSON);

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.evidencesCount()).isEqualTo(0);
            assertThat(result.gratitudesCount()).isEqualTo(0);
            assertThat(result.missionsCompleted()).isEqualTo(0);
            assertThat(result.crisesCount()).isEqualTo(0);
            assertThat(result.ritualsCompleted()).isEqualTo(0);
            assertThat(result.daysActive()).isEqualTo(0);
            assertThat(result.bestStreak()).isEqualTo(0);
            verify(movieRepository).save(any(FamilyMovie.class));
        }

        @Test
        @DisplayName("AI retorna JSON válido → narrativa mapeada correctamente")
        void validAiJson_narrativeMapped() {
            stubGenerate(VALID_JSON);

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.openingLine()).isEqualTo("Un trimestre que cambió todo.");
            assertThat(result.chapter1()).isEqualTo("Se conectaron.");
            assertThat(result.mentorLetter()).isEqualTo("Gracias, familia García.");
            assertThat(result.highlightQuote()).isEqualTo("Juntos siempre.");
        }

        @Test
        @DisplayName("AI retorna JSON inválido → fallback por defecto")
        void invalidAiJson_fallbackNarrative() {
            stubGenerate("esto no es JSON");

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.openingLine()).isEqualTo("Un período que merece ser recordado.");
            assertThat(result.chapter1()).isEqualTo("Esta familia registró momentos significativos.");
        }

        @Test
        @DisplayName("gratitudes fuera del rango → no contadas")
        void gratitudesOutOfRange_notCounted() {
            stubGenerate(VALID_JSON);
            // Fecha anterior al rango
            FamilyGratitudeEntry outOfRange = FamilyGratitudeEntry.builder()
                    .id(1L).fromMember("A").toMember("B").description("X")
                    .createdAt(FROM.minusDays(1).atStartOfDay()).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(outOfRange));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.gratitudesCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("gratitudes dentro del rango → contadas")
        void gratitudesInRange_counted() {
            stubGenerate(VALID_JSON);
            FamilyGratitudeEntry g1 = FamilyGratitudeEntry.builder()
                    .id(1L).fromMember("A").toMember("B").description("G1")
                    .createdAt(FROM.atStartOfDay()).build();
            FamilyGratitudeEntry g2 = FamilyGratitudeEntry.builder()
                    .id(2L).fromMember("C").toMember("D").description("G2")
                    .createdAt(TO.atTime(12, 0)).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(g1, g2));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.gratitudesCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("plan con 3 tareas completadas → missionsCompleted=3")
        void planCompletedTasks_countedInMissions() {
            stubGenerate(VALID_JSON);
            PlanTask t1 = PlanTask.builder().completed(true).build();
            PlanTask t2 = PlanTask.builder().completed(true).build();
            PlanTask t3 = PlanTask.builder().completed(false).build();
            ImprovementPlan plan = ImprovementPlan.builder().build();
            plan.getTasks().addAll(List.of(t1, t2, t3));
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(plan));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.missionsCompleted()).isEqualTo(2);
        }

        @Test
        @DisplayName("sprint con misión COMPLETED en rango → sumada a missionsCompleted")
        void sprintMissionInRange_countedInMissions() {
            stubGenerate(VALID_JSON);
            SprintMission mission = SprintMission.builder()
                    .status("COMPLETED")
                    .completedAt(FROM.plusDays(10).atTime(12, 0))
                    .build();
            FamilySprint sprint = FamilySprint.builder().build();
            sprint.getMissions().add(mission);
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(sprint));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.missionsCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("daysActive: 2 gratitudes en el mismo día → 1 día activo")
        void sameDayGratitudes_onlyOneActiveDay() {
            stubGenerate(VALID_JSON);
            LocalDateTime day1 = FROM.plusDays(5).atTime(9, 0);
            FamilyGratitudeEntry g1 = FamilyGratitudeEntry.builder().id(1L)
                    .fromMember("A").toMember("B").description("X").createdAt(day1).build();
            FamilyGratitudeEntry g2 = FamilyGratitudeEntry.builder().id(2L)
                    .fromMember("C").toMember("D").description("Y").createdAt(day1.plusHours(3)).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(g1, g2));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.daysActive()).isEqualTo(1);
        }

        @Test
        @DisplayName("3 días consecutivos → bestStreak=3")
        void threConsecutiveDays_streakIs3() {
            stubGenerate(VALID_JSON);
            LocalDate d1 = FROM.plusDays(5);
            LocalDate d2 = FROM.plusDays(6);
            LocalDate d3 = FROM.plusDays(7);
            FamilyGratitudeEntry g1 = FamilyGratitudeEntry.builder().id(1L)
                    .fromMember("A").toMember("B").description("X").createdAt(d1.atTime(12, 0)).build();
            FamilyGratitudeEntry g2 = FamilyGratitudeEntry.builder().id(2L)
                    .fromMember("C").toMember("D").description("Y").createdAt(d2.atTime(12, 0)).build();
            FamilyGratitudeEntry g3 = FamilyGratitudeEntry.builder().id(3L)
                    .fromMember("E").toMember("F").description("Z").createdAt(d3.atTime(12, 0)).build();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(g1, g2, g3));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.bestStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("ritualRepository solo filtra COMPLETED con completedAt en rango")
        void onlyCompletedRitualsInRange_counted() {
            stubGenerate(VALID_JSON);
            LocalDateTime inRange = FROM.plusDays(15).atTime(12, 0);
            FamilyRitual completed = FamilyRitual.builder().id(1L).familyId(FAM_ID)
                    .title("Ritual completado").status(RitualStatus.COMPLETED)
                    .completedAt(inRange).build();
            FamilyRitual pending = FamilyRitual.builder().id(2L).familyId(FAM_ID)
                    .title("Pendiente").status(RitualStatus.PENDING)
                    .completedAt(null).build();
            when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(FAM_ID)).thenReturn(List.of(completed, pending));

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.ritualsCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("familyName del DTO propagado desde family.getName()")
        void familyName_propagatedToDto() {
            stubGenerate(VALID_JSON);

            FamilyMovieDto result = service.generate(FAM_ID, FROM, TO);

            assertThat(result.familyName()).isEqualTo("García");
        }

        @Test
        @DisplayName("periodLabel: mismo mes → nombre completo del mes")
        void periodLabel_sameMonth_fullMonthName() {
            LocalDate jan1 = LocalDate.of(2026, 1, 1);
            LocalDate jan31 = LocalDate.of(2026, 1, 31);

            when(movieRepository.existsByFamilyIdAndPeriodStartAndPeriodEnd(FAM_ID, jan1, jan31)).thenReturn(false);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            when(ritualRepository.findByFamilyIdOrderByTriggeredAtDesc(FAM_ID)).thenReturn(List.of());
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            when(aiProvider.generateRawResponse(any())).thenReturn(VALID_JSON);
            when(movieRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyMovieDto result = service.generate(FAM_ID, jan1, jan31);

            assertThat(result.periodLabel()).contains("enero").contains("2026");
        }
    }
}
