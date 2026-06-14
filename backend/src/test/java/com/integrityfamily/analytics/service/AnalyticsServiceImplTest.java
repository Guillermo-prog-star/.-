package com.integrityfamily.analytics.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.risk.service.FamilyCausalEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl")
class AnalyticsServiceImplTest {

    @Mock FamilyRepository                                          familyRepository;
    @Mock EvaluationRepository                                      evaluationRepository;
    @Mock AiService                                                 aiService;
    @Mock RiskSnapshotRepository                                    riskSnapshotRepository;
    @Mock ChecklistItemRepository                                   checklistRepository;
    @Mock FamilyLogbookRepository                                   logbookRepository;
    @Mock ImprovementPlanRepository                                 planRepository;
    @Mock PlanTaskRepository                                        planTaskRepository;
    @Mock RabbitTemplate                                            rabbitTemplate;
    @Mock com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository longitudinalStateRepository;
    @Mock FamilyCausalEngine                                        causalEngine;

    @InjectMocks AnalyticsServiceImpl service;

    private final Family DEFAULT_FAMILY = Family.builder()
            .id(1L).name("Familia Test").familyCode("FAM-001")
            .currentMilestone("W1").build();

    @BeforeEach
    void stubDefaults() {
        // Stubs comunes para calculateLatestResults (lenient porque no todos los tests los usan)
        lenient().when(familyRepository.findById(anyLong())).thenReturn(Optional.of(DEFAULT_FAMILY));
        lenient().when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(anyLong())).thenReturn(List.of());
        lenient().when(riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(Optional.empty());
        lenient().when(checklistRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(logbookRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(anyLong(), any())).thenReturn(List.of());
        lenient().when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(longitudinalStateRepository.findByFamilyId(anyLong())).thenReturn(Optional.empty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Evaluation eval(Long id, Double icf) {
        return Evaluation.builder().id(id).icf(icf).build();
    }

    private FamilyLogbookEntry openEntry() {
        FamilyLogbookEntry e = new FamilyLogbookEntry();
        e.setStatus(LogbookStatus.OPEN);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getEvolutionRadarData
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getEvolutionRadarData")
    class GetEvolutionRadarData {

        @Test
        @DisplayName("sin historial → retorna dos series con valores 0.0 (Actual e Inicio)")
        void noHistory_returnsZeroSeries() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(10L)).thenReturn(List.of());

            List<java.util.Map<String, Object>> data = service.getEvolutionRadarData(10L);

            assertThat(data).hasSize(2);
            assertThat(data.get(0).get("name")).isEqualTo("Actual");
            assertThat(data.get(1).get("name")).isEqualTo("Inicio");

            @SuppressWarnings("unchecked")
            List<Double> actualValues = (List<Double>) data.get(0).get("value");
            assertThat(actualValues).containsExactly(0.0, 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("con evaluaciones → Actual usa lastEval, Inicio usa firstEval, dims sin match → 50.0")
        void withHistory_mapsFirstAndLast() {
            Evaluation first = eval(1L, 55.0);
            Evaluation last  = eval(2L, 70.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(10L))
                    .thenReturn(List.of(first, last));

            List<java.util.Map<String, Object>> data = service.getEvolutionRadarData(10L);

            assertThat(data).hasSize(2);
            assertThat(data.get(0).get("name")).isEqualTo("Actual");
            // Sin dimensionScores → cada score es 50.0 (default)
            @SuppressWarnings("unchecked")
            List<Double> actualValues = (List<Double>) data.get(0).get("value");
            assertThat(actualValues).containsExactly(50.0, 50.0, 50.0, 50.0);
        }

        @Test
        @DisplayName("dimensionScores con nombre 'comunicacion' → retorna su score real")
        void withDimensionScores_correctMapping() {
            EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                    .dimensionName("comunicacion").score(75.0).build();
            Evaluation last = Evaluation.builder().id(1L).icf(65.0)
                    .dimensionScores(List.of(ds)).build();
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(10L))
                    .thenReturn(List.of(last));

            List<java.util.Map<String, Object>> data = service.getEvolutionRadarData(10L);

            @SuppressWarnings("unchecked")
            List<Double> actual = (List<Double>) data.get(0).get("value");
            // [emociones=50.0, comunicacion=75.0, habitos=50.0, tiempos=50.0]
            assertThat(actual.get(1)).isEqualTo(75.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateLatestResults — awarenessGrowth
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateLatestResults — crecimiento de consciencia")
    class AwarenessGrowth {

        @Test
        @DisplayName("firstEval.icf=50, lastEval.icf=70 → awarenessGrowth=20.0")
        void growthCalculation() {
            Evaluation first = eval(1L, 50.0);
            Evaluation last  = eval(2L, 70.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(first, last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.awarenessGrowth()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("sin evaluaciones → awarenessGrowth=0.0")
        void noEvals_zeroGrowth() {
            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.awarenessGrowth()).isEqualTo(0.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateLatestResults — sentinel activo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateLatestResults — motor sentinel")
    class SentinelLogic {

        @Test
        @DisplayName("ICF < 40 → isSentinelActive=true")
        void icfBelow40_sentinelActive() {
            Evaluation last = eval(1L, 35.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.isSentinelActive()).isTrue();
            assertThat(r.hasCrisis()).isTrue();
            // El insight debe tener el prefijo de S.O.S
            assertThat(r.aiRecommendation()).startsWith("⚠️ [S.O.S NODO]");
        }

        @Test
        @DisplayName("growth < -15 → isSentinelActive=true")
        void growthBelow15_sentinelActive() {
            Evaluation first = eval(1L, 70.0);
            Evaluation last  = eval(2L, 50.0); // growth = -20
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(first, last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.isSentinelActive()).isTrue();
        }

        @Test
        @DisplayName("openLogbookItems > 3 → isSentinelActive=true")
        void openLogbook4_sentinelActive() {
            // 4 entradas OPEN
            List<FamilyLogbookEntry> open = List.of(
                    openEntry(), openEntry(), openEntry(), openEntry());
            when(logbookRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(1L, LogbookStatus.OPEN))
                    .thenReturn(open);

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.isSentinelActive()).isTrue();
        }

        @Test
        @DisplayName("condiciones normales (ICF=65, growth=+10, 0 open) → isSentinelActive=false")
        void normalConditions_sentinelInactive() {
            Evaluation first = eval(1L, 55.0);
            Evaluation last  = eval(2L, 65.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(first, last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.isSentinelActive()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateLatestResults — pillarProgress
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateLatestResults — progreso de pilar")
    class PillarProgress {

        @Test
        @DisplayName("4 tareas totales, 2 completadas → pillarProgress=50.0%")
        void pillarProgress50Percent() {
            when(planTaskRepository.countByFamilyId(1L)).thenReturn(4L);
            when(planTaskRepository.countCompletedByFamilyId(1L)).thenReturn(2L);

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.pillarProgress()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("sin tareas → pillarProgress=0.0 (evita división por cero)")
        void noTasks_zeroPillarProgress() {
            // planTaskRepository.countByFamilyId devuelve 0 por defecto

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.pillarProgress()).isEqualTo(0.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateLatestResults — nivel de consciencia
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateLatestResults — nivel de consciencia")
    class ConsciousnessLevel {

        @Test
        @DisplayName("RiskSnapshot con consciousnessLevel=3 → se usa directamente (no se deriva)")
        void consciousnessFromSnapshot() {
            RiskSnapshot snap = new RiskSnapshot();
            snap.setConsciousnessLevel(3);
            snap.setConsciousnessLabel("Consciente");
            when(riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Optional.of(snap));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.latestConsciousnessLevel()).isEqualTo(3);
            assertThat(r.latestConsciousnessLabel()).isEqualTo("Consciente");
        }

        @Test
        @DisplayName("sin snapshot, ICF=65 → deriveConsciousnessLevel: 60≤65<80 → nivel 4, Madurando")
        void consciousnessDerivedFromIcf_level4() {
            Evaluation last = eval(1L, 65.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.latestConsciousnessLevel()).isEqualTo(4);
            assertThat(r.latestConsciousnessLabel()).isEqualTo("Madurando");
        }

        @Test
        @DisplayName("sin snapshot, ICF=35 → nivel 2, Reactiva (20≤35<40)")
        void consciousnessDerivedFromIcf_level2() {
            Evaluation last = eval(1L, 35.0);
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                    .thenReturn(List.of(last));

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.latestConsciousnessLevel()).isEqualTo(2);
            assertThat(r.latestConsciousnessLabel()).isEqualTo("Reactiva");
        }

        @Test
        @DisplayName("sin snapshot ni evaluación → ICF=0 → nivel 1, Inconsciente")
        void noEvalNoSnapshot_level1() {
            // defaults: evaluations=[], snapshot=empty

            DashboardSummaryResponse r = service.calculateLatestResults(1L);

            assertThat(r.latestConsciousnessLevel()).isEqualTo(1);
            assertThat(r.latestConsciousnessLabel()).isEqualTo("Inconsciente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // calculateLatestResults — familia no encontrada
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateLatestResults — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("findById vacío y findAll vacío → lanza BusinessException")
        void familyNotFound_throwsBusinessException() {
            when(familyRepository.findById(999L)).thenReturn(Optional.empty());
            when(familyRepository.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> service.calculateLatestResults(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("familia");
        }
    }
}
