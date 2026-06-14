package com.integrityfamily.context.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.context.domain.FamilyContextSnapshot;
import com.integrityfamily.context.dto.FamilyContextDto;
import com.integrityfamily.context.repository.FamilyContextRepository;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.repository.FamilyRitualRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyContextEngine")
class FamilyContextEngineTest {

    @Mock FamilyContextRepository               contextRepository;
    @Mock FamilyRepository                      familyRepository;
    @Mock FamilyLongitudinalStateRepository     ltsRepository;
    @Mock FamilyGratitudeEntryRepository        gratitudeRepository;
    @Mock TaskEvidenceRepository                evidenceRepository;
    @Mock FamilyLogbookRepository               logbookRepository;
    @Mock CriticalDayRepository                 crisisRepository;
    @Mock FamilySprintRepository                sprintRepository;
    @Mock FamilyRitualRepository                ritualRepository;
    @Spy  ObjectMapper                          objectMapper = new ObjectMapper();

    @InjectMocks FamilyContextEngine engine;

    private static final long FAM = 1L;
    private static final Family DEFAULT_FAMILY =
            Family.builder().id(FAM).name("Familia Test").build();

    @BeforeEach
    void stubDefaults() {
        lenient().when(familyRepository.findById(anyLong())).thenReturn(Optional.of(DEFAULT_FAMILY));
        lenient().when(contextRepository.findByFamilyId(anyLong())).thenReturn(Optional.empty());
        lenient().when(ltsRepository.findByFamilyId(anyLong())).thenReturn(Optional.empty());
        lenient().when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(evidenceRepository.findByFamilyId(anyLong())).thenReturn(List.of());
        lenient().when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(sprintRepository.findActiveSprintForFamily(anyLong())).thenReturn(Optional.empty());
        lenient().when(ritualRepository.findByFamilyIdAndStatusOrderByTriggeredAtDesc(anyLong(), any()))
                 .thenReturn(List.of());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FamilyGratitudeEntry gratitude(LocalDateTime at) {
        return FamilyGratitudeEntry.builder().createdAt(at).build();
    }

    private FamilyLongitudinalState lts(java.util.function.Consumer<FamilyLongitudinalState.FamilyLongitudinalStateBuilder> cfg) {
        FamilyLongitudinalState.FamilyLongitudinalStateBuilder b = FamilyLongitudinalState.builder();
        cfg.accept(b);
        return b.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // familia no encontrada
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("familia no encontrada → IllegalArgumentException")
    void familyNotFound_throws() {
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.compute(99L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // connectionLevel
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("connectionLevel")
    class ConnectionLevel {

        @Test
        @DisplayName("7 gratitudes en últimos 7 días → ALTA")
        void sevenEventsInWeek_alta() {
            LocalDateTime recent = LocalDateTime.now().minusDays(1);
            List<FamilyGratitudeEntry> gs = IntStream.range(0, 7)
                    .mapToObj(i -> gratitude(recent)).toList();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM)).thenReturn(gs);

            assertThat(engine.compute(FAM, false).connectionLevel()).isEqualTo("ALTA");
        }

        @Test
        @DisplayName("3 gratitudes en últimos 7 días → MEDIA")
        void threeEventsInWeek_media() {
            LocalDateTime recent = LocalDateTime.now().minusDays(1);
            List<FamilyGratitudeEntry> gs = IntStream.range(0, 3)
                    .mapToObj(i -> gratitude(recent)).toList();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM)).thenReturn(gs);

            assertThat(engine.compute(FAM, false).connectionLevel()).isEqualTo("MEDIA");
        }

        @Test
        @DisplayName("sin entradas en ningún repositorio → BAJA")
        void noEvents_baja() {
            assertThat(engine.compute(FAM, false).connectionLevel()).isEqualTo("BAJA");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // stressLevel
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("stressLevel")
    class StressLevel {

        @Test
        @DisplayName("LTS con crisis < 48h (isInActiveCrisis=true) → CRITICO")
        void recentCrisis_critico() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .lastCrisisAt(LocalDateTime.now().minusHours(2)).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).stressLevel()).isEqualTo("CRITICO");
        }

        @Test
        @DisplayName("LTS con communicationCollapseActive=true → ALTO")
        void collapseActive_alto() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .communicationCollapseActive(true).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).stressLevel()).isEqualTo("ALTO");
        }

        @Test
        @DisplayName("LTS con crisisCount30d=2 → ALTO")
        void crisisCount2_alto() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .crisisCount30d(2).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).stressLevel()).isEqualTo("ALTO");
        }

        @Test
        @DisplayName("LTS con crisisCount30d=1 → MODERADO")
        void crisisCount1_moderado() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .crisisCount30d(1).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).stressLevel()).isEqualTo("MODERADO");
        }

        @Test
        @DisplayName("sin LTS ni crisis → BAJO")
        void noLtsNoCrisis_bajo() {
            assertThat(engine.compute(FAM, false).stressLevel()).isEqualTo("BAJO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // communicationTrend
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("communicationTrend")
    class CommunicationTrendTest {

        @Test
        @DisplayName("consecutiveImprovements=2 → MEJORANDO")
        void improvements2_mejorando() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .consecutiveImprovements(2).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).communicationTrend()).isEqualTo("MEJORANDO");
        }

        @Test
        @DisplayName("consecutiveDeteriorations=2 → DETERIORANDO")
        void deteriorations2_deteriorando() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .consecutiveDeteriorations(2).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).communicationTrend()).isEqualTo("DETERIORANDO");
        }

        @Test
        @DisplayName("sin LTS → ESTABLE")
        void noLts_estable() {
            assertThat(engine.compute(FAM, false).communicationTrend()).isEqualTo("ESTABLE");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // participationLevel
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("participationLevel")
    class ParticipationLevelTest {

        @Test
        @DisplayName("familia sin miembros activos → BAJA")
        void noActiveMembers_baja() {
            // DEFAULT_FAMILY tiene 0 miembros (ArrayList vacío por @Builder.Default)
            assertThat(engine.compute(FAM, false).participationLevel()).isEqualTo("BAJA");
        }

        @Test
        @DisplayName("miembro activo + última actividad hace 20 días → BAJA")
        void activeMemberButLongInactive_baja() {
            Family family = Family.builder().id(FAM).name("T")
                    .members(List.of(FamilyMember.builder().build())).build();
            when(familyRepository.findById(FAM)).thenReturn(Optional.of(family));
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM))
                    .thenReturn(List.of(gratitude(LocalDateTime.now().minusDays(20))));

            assertThat(engine.compute(FAM, false).participationLevel()).isEqualTo("BAJA");
        }

        @Test
        @DisplayName("miembro activo + actividad hoy → ALTA")
        void activeMemberAndRecentActivity_alta() {
            Family family = Family.builder().id(FAM).name("T")
                    .members(List.of(FamilyMember.builder().build())).build();
            when(familyRepository.findById(FAM)).thenReturn(Optional.of(family));
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM))
                    .thenReturn(List.of(gratitude(LocalDateTime.now())));

            assertThat(engine.compute(FAM, false).participationLevel()).isEqualTo("ALTA");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // overallMood
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("overallMood")
    class OverallMoodTest {

        @Test
        @DisplayName("stress=CRITICO → EN_CRISIS (prioridad máxima)")
        void stressCritico_enCrisis() {
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .lastCrisisAt(LocalDateTime.now().minusHours(1)).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(engine.compute(FAM, false).overallMood()).isEqualTo("EN_CRISIS");
        }

        @Test
        @DisplayName("streak=7 días consecutivos + connectionLevel=ALTA → CELEBRANDO")
        void streak7AndAlta_celebrando() {
            // 7 gratitudes en días 0..6 (hoy, ayer, ...) → streak=7, connectionLevel=ALTA
            List<FamilyGratitudeEntry> gs = IntStream.range(0, 7)
                    .mapToObj(i -> gratitude(LocalDateTime.now().minusDays(i))).toList();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM)).thenReturn(gs);

            FamilyContextDto ctx = engine.compute(FAM, false);

            assertThat(ctx.currentStreak()).isGreaterThanOrEqualTo(7);
            assertThat(ctx.connectionLevel()).isEqualTo("ALTA");
            assertThat(ctx.overallMood()).isEqualTo("CELEBRANDO");
        }

        @Test
        @DisplayName("trend=ASCENDENTE + connectionLevel=ALTA (streak=0) → CRECIENDO")
        void ascendenteTrendAndAlta_creciendo() {
            // 7 gratitudes todas ayer → connectionLevel=ALTA, streak=0 (no hay actividad hoy)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<FamilyGratitudeEntry> gs = IntStream.range(0, 7)
                    .mapToObj(i -> gratitude(yesterday)).toList();
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM)).thenReturn(gs);
            FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                    .riskTrend("IMPROVING").build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            FamilyContextDto ctx = engine.compute(FAM, false);

            assertThat(ctx.overallTrend()).isEqualTo("ASCENDENTE");
            assertThat(ctx.overallMood()).isEqualTo("CRECIENDO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // save flag
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("compute con save=false → contextRepository.save() no es invocado")
    void saveFalse_noSaveCall() {
        engine.compute(FAM, false);

        verify(contextRepository, never()).save(any());
    }

    @Test
    @DisplayName("compute con save=true → contextRepository.save() es invocado")
    void saveTrue_saveIsCalled() {
        engine.compute(FAM, true);

        verify(contextRepository).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // daysWithoutActivity
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sin ninguna actividad registrada → daysWithoutActivity=999")
    void noActivity_returns999() {
        assertThat(engine.compute(FAM, false).daysWithoutActivity()).isEqualTo(999);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // alerts
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LTS en crisis activa → alerta de crisis presente")
    void activeCrisis_alertPresent() {
        FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                .lastCrisisAt(LocalDateTime.now().minusHours(1)).build();
        when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

        assertThat(engine.compute(FAM, false).alerts())
                .anyMatch(a -> a.contains("Crisis familiar activa"));
    }

    @Test
    @DisplayName("última actividad hace 20 días → alerta de inactividad presente")
    void inactivity20Days_alertPresent() {
        when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(FAM))
                .thenReturn(List.of(gratitude(LocalDateTime.now().minusDays(20))));

        assertThat(engine.compute(FAM, false).alerts())
                .anyMatch(a -> a.contains("días sin actividad registrada"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // recommendations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("connectionLevel=BAJA → recomendaciones incluyen actividad presencial")
    void lowConnection_recommendationPresent() {
        // sin entradas → connectionLevel=BAJA
        List<String> recs = engine.compute(FAM, false).recommendations();

        assertThat(recs).anyMatch(r -> r.contains("actividad presencial"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // overallTrend (mapTrend)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LTS.riskTrend='DETERIORATING' → overallTrend='DESCENDENTE'")
    void deterioratingTrend_descendente() {
        FamilyLongitudinalState s = FamilyLongitudinalState.builder()
                .riskTrend("DETERIORATING").build();
        when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(s));

        assertThat(engine.compute(FAM, false).overallTrend()).isEqualTo("DESCENDENTE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildContextBlock
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildContextBlock")
    class BuildContextBlock {

        @Test
        @DisplayName("sin snapshot en repositorio → retorna null")
        void noSnapshot_returnsNull() {
            // lenient default ya devuelve Optional.empty()
            assertThat(engine.buildContextBlock(FAM)).isNull();
        }

        @Test
        @DisplayName("snapshot existente → bloque formateado con todos los campos")
        void withSnapshot_formattedBlock() {
            FamilyContextSnapshot snap = FamilyContextSnapshot.builder()
                    .familyId(FAM)
                    .connectionLevel("ALTA")
                    .stressLevel("BAJO")
                    .communicationTrend("MEJORANDO")
                    .participationLevel("ALTA")
                    .overallTrend("ASCENDENTE")
                    .overallMood("CELEBRANDO")
                    .currentStreak(5)
                    .daysWithoutActivity(1)
                    .build();
            when(contextRepository.findByFamilyId(FAM)).thenReturn(Optional.of(snap));

            String block = engine.buildContextBlock(FAM);

            assertThat(block).contains("Estado Familiar Actual:");
            assertThat(block).contains("ALTA");
            assertThat(block).contains("BAJO");
            assertThat(block).contains("MEJORANDO");
            assertThat(block).contains("Racha actual: 5 días");
            assertThat(block).contains("Sin actividad: 1 días");
        }
    }
}
