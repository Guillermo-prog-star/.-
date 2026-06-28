package com.integrityfamily.capital.service;

import com.integrityfamily.capital.dto.IndicatorResult;
import com.integrityfamily.capital.dto.IndicatorsSnapshot;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyIcafAnswer;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FamilyIndicatorsService — Unit Tests")
class FamilyIndicatorsServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────────
    @Mock FamilyLongitudinalStateRepository longitudinalRepo;
    @Mock SprintDailyRepository             sprintDailyRepo;
    @Mock FamilyGratitudeEntryRepository    gratitudeRepo;
    @Mock FamilyBehavioralEventRepository   behavioralRepo;
    @Mock FamilyIcafAnswerRepository        icafAnswerRepo;
    @Mock MemberRepository                  memberRepo;
    @Mock TaskEvidenceRepository            evidenceRepo;
    @Mock SprintMissionRepository           missionRepo;
    @Mock FamilySprintRepository            sprintRepo;
    @Mock SprintRetrospectiveRepository     retroRepo;
    @Mock EvaluationRepository              evaluationRepo;
    @Mock PlanTaskRepository                planTaskRepo;
    @Mock FamilyCriticalEventRepository     criticalEventRepo;

    @InjectMocks FamilyIndicatorsService service;

    private static final Long FAM = 1L;

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Estado longitudinal con ICF e ICaF completos */
    private FamilyLongitudinalState fullState() {
        FamilyLongitudinalState s = new FamilyLongitudinalState();
        s.setIcfCurrent(72.0);
        s.setIcafCurrent(68.0);
        s.setIcaf6mAgo(55.0);
        s.setIcaf12mAgo(45.0);
        s.setIcafMadurez(4);
        s.setIcafTrend("IMPROVING");
        return s;
    }

    /** Stubs mínimos para que todos los demás indicadores devuelvan fallback
     *  sin arrojar NullPointerException */
    private void stubAllFallbacks() {
        when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.empty());
        when(sprintRepo.findActiveAndCompletedByFamilyId(FAM)).thenReturn(List.of());
        when(gratitudeRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(0L);
        when(behavioralRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(0L);
        when(behavioralRepo.countRepairedByFamilyIdSince(eq(FAM), any())).thenReturn(0L);
        when(icafAnswerRepo.countAnsweredByDomain(eq(FAM), anyString())).thenReturn(0L);
        when(icafAnswerRepo.findByFamilyIdAndQuestionKey(eq(FAM), anyString())).thenReturn(Optional.empty());
        when(memberRepo.countByFamilyId(FAM)).thenReturn(0L);
        when(sprintDailyRepo.countDistinctMembersWithDailySince(eq(FAM), any())).thenReturn(0L);
        when(evidenceRepo.countDistinctSubmittersSince(eq(FAM), any())).thenReturn(0L);
        when(icafAnswerRepo.countDistinctRespondersSince(eq(FAM), any())).thenReturn(0L);
        when(planTaskRepo.countByFamilyId(FAM)).thenReturn(0L);
        when(planTaskRepo.countCompletedByFamilyId(FAM)).thenReturn(0L);
        when(missionRepo.countCompletedByFamilyId(FAM)).thenReturn(0L);
        when(evidenceRepo.countValidatedByFamilyId(FAM)).thenReturn(0L);
        when(sprintRepo.countByFamilyId(FAM)).thenReturn(0L);
        when(retroRepo.countByFamilyId(FAM)).thenReturn(0L);
        when(evaluationRepo.findByFamilyIdOrderByFinalizedAtAsc(FAM)).thenReturn(List.of());
        when(planTaskRepo.avgImpactoIcfCompletedByFamilyId(FAM)).thenReturn(null);
        when(criticalEventRepo.countByFamilyIdAndStatus(eq(FAM), anyString())).thenReturn(0L);
        when(criticalEventRepo.countActiveByFamilyId(FAM)).thenReturn(0L);
        when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM)).thenReturn(0.0);
        when(criticalEventRepo.totalRelapsesByFamilyId(FAM)).thenReturn(0L);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SNAPSHOT COMPLETO
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("computeAll()")
    class ComputeAll {

        @Test
        @DisplayName("devuelve exactamente 20 indicadores")
        void devuelve20Indicadores() {
            stubAllFallbacks();

            IndicatorsSnapshot snap = service.computeAll(FAM);

            assertThat(snap.indicators()).hasSize(20);
            assertThat(snap.familyId()).isEqualTo(FAM);
            assertThat(snap.calculatedAt()).isNotNull();
        }

        @Test
        @DisplayName("los IDs van de IND-01 a IND-20 en orden")
        void idsEnOrden() {
            stubAllFallbacks();

            List<String> ids = service.computeAll(FAM).indicators()
                    .stream().map(IndicatorResult::id).toList();

            assertThat(ids).containsExactly(
                    "IND-01","IND-02","IND-03","IND-04",
                    "IND-05","IND-06","IND-07",
                    "IND-08","IND-09","IND-10","IND-11","IND-12",
                    "IND-13","IND-14","IND-15","IND-16",
                    "IND-17","IND-18","IND-19","IND-20"
            );
        }

        @Test
        @DisplayName("todos los valores están entre 0 y 100")
        void todosLosValoresEnRango() {
            stubAllFallbacks();

            service.computeAll(FAM).indicators().forEach(r ->
                    assertThat(r.value())
                            .as("IND %s fuera de rango: %s", r.id(), r.value())
                            .isBetween(0.0, 100.0));
        }

        @Test
        @DisplayName("smffScore es la media de los 20 valores")
        void smffScoreEsMedia() {
            stubAllFallbacks();

            IndicatorsSnapshot snap = service.computeAll(FAM);
            double media = snap.indicators().stream()
                    .mapToDouble(IndicatorResult::value).average().orElse(0);

            assertThat(snap.smffScore()).isCloseTo(media, within(0.01));
        }

        @Test
        @DisplayName("sin datos → dataCompletePct = 0 o bajo (todo estimado)")
        void sinDatosTodoEstimado() {
            stubAllFallbacks();

            IndicatorsSnapshot snap = service.computeAll(FAM);
            // IND-03 (gratitud 0) y IND-08/09/10/11 (sin plan/sprint)
            // no son estimados sino valores reales en 0 — verificamos
            // que la completitud sea menor al 50%
            assertThat(snap.dataCompletePct()).isLessThanOrEqualTo(50.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRUPO 1 — COHESIÓN Y VÍNCULO
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IND-01: Índice de Cohesión (ICF)")
    class Ind01 {

        @Test
        @DisplayName("con estado longitudinal → valor = icfCurrent")
        void conEstado() {
            stubAllFallbacks();
            when(longitudinalRepo.findByFamilyId(FAM))
                    .thenReturn(Optional.of(fullState()));

            IndicatorResult r = indicator("IND-01");

            assertThat(r.value()).isEqualTo(72.0);
            assertThat(r.isEstimated()).isFalse();
        }

        @Test
        @DisplayName("sin estado → estimado=true, valor=50")
        void sinEstado() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-01");

            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("IND-02: Constancia en Dailies")
    class Ind02 {

        @Test
        @DisplayName("sin sprints → estimado=true")
        void sinSprints() {
            stubAllFallbacks();

            assertThat(indicator("IND-02").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("50% de dailies realizados → valor = 50")
        void mitadDailies() {
            stubAllFallbacks();
            FamilySprint sprint = sprintConFechas(
                    LocalDate.now().minusDays(9), LocalDate.now().minusDays(0));
            when(sprintRepo.findActiveAndCompletedByFamilyId(FAM)).thenReturn(List.of(sprint));
            when(memberRepo.countByFamilyId(FAM)).thenReturn(2L);
            // 10 días × 2 miembros = 20 esperados; realizados = 10
            when(sprintDailyRepo.countByFamilyIdAndDateRange(eq(FAM), any(), any()))
                    .thenReturn(10L);

            IndicatorResult r = indicator("IND-02");

            assertThat(r.value()).isCloseTo(50.0, within(0.1));
            assertThat(r.isEstimated()).isFalse();
        }

        @Test
        @DisplayName("100% dailies → valor = 100 (cap)")
        void todosDailies() {
            stubAllFallbacks();
            FamilySprint sprint = sprintConFechas(
                    LocalDate.now().minusDays(6), LocalDate.now());
            when(sprintRepo.findActiveAndCompletedByFamilyId(FAM)).thenReturn(List.of(sprint));
            when(memberRepo.countByFamilyId(FAM)).thenReturn(1L);
            when(sprintDailyRepo.countByFamilyIdAndDateRange(eq(FAM), any(), any()))
                    .thenReturn(7L); // 7 días × 1 miembro = 7 esperados
            // retornamos más para verificar cap
            when(sprintDailyRepo.countByFamilyIdAndDateRange(eq(FAM), any(), any()))
                    .thenReturn(10L);

            assertThat(indicator("IND-02").value()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("IND-03: Frecuencia de Gratitud")
    class Ind03 {

        @Test
        @DisplayName("0 entradas → valor = 0 (estimado)")
        void sinEntradas() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-03");

            assertThat(r.value()).isEqualTo(0.0);
            assertThat(r.isEstimated()).isTrue();
        }

        @Test
        @DisplayName("8 entradas (objetivo) → valor = 100")
        void objetivoCumplido() {
            stubAllFallbacks();
            when(gratitudeRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(8L);

            IndicatorResult r = indicator("IND-03");

            assertThat(r.value()).isEqualTo(100.0);
            assertThat(r.isEstimated()).isFalse();
        }

        @Test
        @DisplayName("4 entradas → valor = 50")
        void mitadObjetivo() {
            stubAllFallbacks();
            when(gratitudeRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(4L);

            assertThat(indicator("IND-03").value()).isCloseTo(50.0, within(0.1));
        }

        @Test
        @DisplayName("16 entradas → valor cap = 100")
        void sobreObjetivoCap() {
            stubAllFallbacks();
            when(gratitudeRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(16L);

            assertThat(indicator("IND-03").value()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("IND-04: Reparación Conductual")
    class Ind04 {

        @Test
        @DisplayName("sin eventos → estimado, valor=50")
        void sinEventos() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-04");

            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("3 de 4 eventos reparados → valor = 75")
        void tresCuartos() {
            stubAllFallbacks();
            when(behavioralRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(4L);
            when(behavioralRepo.countRepairedByFamilyIdSince(eq(FAM), any())).thenReturn(3L);

            assertThat(indicator("IND-04").value()).isCloseTo(75.0, within(0.1));
        }

        @Test
        @DisplayName("0 de 2 reparados → valor = 0")
        void ninguno() {
            stubAllFallbacks();
            when(behavioralRepo.countByFamilyIdSince(eq(FAM), any())).thenReturn(2L);
            when(behavioralRepo.countRepairedByFamilyIdSince(eq(FAM), any())).thenReturn(0L);

            assertThat(indicator("IND-04").value()).isEqualTo(0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRUPO 2 — CONFIANZA
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IND-05: Confianza Interpersonal")
    class Ind05 {

        @Test
        @DisplayName("sin respuestas → estimado, valor=50")
        void sinRespuestas() {
            stubAllFallbacks();

            assertThat(indicator("IND-05").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("avg=5 (máximo) → valor=100")
        void maximo() {
            stubAllFallbacks();
            when(icafAnswerRepo.countAnsweredByDomain(eq(FAM), eq("confianza"))).thenReturn(7L);
            when(icafAnswerRepo.avgScoreByDomain(eq(FAM), eq("confianza"))).thenReturn(5.0);

            assertThat(indicator("IND-05").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("avg=1 (mínimo) → valor=0")
        void minimo() {
            stubAllFallbacks();
            when(icafAnswerRepo.countAnsweredByDomain(eq(FAM), eq("confianza"))).thenReturn(7L);
            when(icafAnswerRepo.avgScoreByDomain(eq(FAM), eq("confianza"))).thenReturn(1.0);

            assertThat(indicator("IND-05").value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("avg=3 (punto medio) → valor≈50")
        void puntoDMedio() {
            stubAllFallbacks();
            when(icafAnswerRepo.countAnsweredByDomain(eq(FAM), eq("confianza"))).thenReturn(7L);
            when(icafAnswerRepo.avgScoreByDomain(eq(FAM), eq("confianza"))).thenReturn(3.0);

            assertThat(indicator("IND-05").value()).isCloseTo(50.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("IND-06: Apertura Comunicacional")
    class Ind06 {

        @Test
        @DisplayName("sin respuesta ICAF_CONF_007 → estimado")
        void sinRespuesta() {
            stubAllFallbacks();

            assertThat(indicator("IND-06").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("score=1 (máxima apertura) → valor=100")
        void maximaApertura() {
            stubAllFallbacks();
            FamilyIcafAnswer ans = answer("ICAF_CONF_007", 1);
            when(icafAnswerRepo.findByFamilyIdAndQuestionKey(FAM, "ICAF_CONF_007"))
                    .thenReturn(Optional.of(ans));

            assertThat(indicator("IND-06").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("score=5 (temas tabú totales) → valor=0")
        void tematabu() {
            stubAllFallbacks();
            FamilyIcafAnswer ans = answer("ICAF_CONF_007", 5);
            when(icafAnswerRepo.findByFamilyIdAndQuestionKey(FAM, "ICAF_CONF_007"))
                    .thenReturn(Optional.of(ans));

            assertThat(indicator("IND-06").value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("IND-07: Participación Multi-Miembro")
    class Ind07 {

        @Test
        @DisplayName("sin miembros → estimado")
        void sinMiembros() {
            stubAllFallbacks();

            assertThat(indicator("IND-07").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("2 de 4 miembros activos → valor=50")
        void mitadActivos() {
            stubAllFallbacks();
            when(memberRepo.countByFamilyId(FAM)).thenReturn(4L);
            when(sprintDailyRepo.countDistinctMembersWithDailySince(eq(FAM), any())).thenReturn(2L);

            assertThat(indicator("IND-07").value()).isCloseTo(50.0, within(0.1));
        }

        @Test
        @DisplayName("más activos que miembros → cap=100")
        void sobreCap() {
            stubAllFallbacks();
            when(memberRepo.countByFamilyId(FAM)).thenReturn(2L);
            when(sprintDailyRepo.countDistinctMembersWithDailySince(eq(FAM), any())).thenReturn(3L);

            assertThat(indicator("IND-07").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("prioriza el máximo entre las 3 fuentes")
        void maxDe3Fuentes() {
            stubAllFallbacks();
            when(memberRepo.countByFamilyId(FAM)).thenReturn(4L);
            when(sprintDailyRepo.countDistinctMembersWithDailySince(eq(FAM), any())).thenReturn(1L);
            when(evidenceRepo.countDistinctSubmittersSince(eq(FAM), any())).thenReturn(3L);
            when(icafAnswerRepo.countDistinctRespondersSince(eq(FAM), any())).thenReturn(2L);

            assertThat(indicator("IND-07").value()).isCloseTo(75.0, within(0.1)); // 3/4
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRUPO 3 — TRANSFORMACIÓN ACTIVA
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IND-08: Completitud del Plan")
    class Ind08 {

        @Test
        @DisplayName("sin plan → valor=0 (no estimado, dato real)")
        void sinPlan() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-08");
            assertThat(r.value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("6/10 tareas completadas → valor=60")
        void seisDeDiez() {
            stubAllFallbacks();
            when(planTaskRepo.countByFamilyId(FAM)).thenReturn(10L);
            when(planTaskRepo.countCompletedByFamilyId(FAM)).thenReturn(6L);

            assertThat(indicator("IND-08").value()).isCloseTo(60.0, within(0.1));
        }

        @Test
        @DisplayName("todas completas → valor=100")
        void todas() {
            stubAllFallbacks();
            when(planTaskRepo.countByFamilyId(FAM)).thenReturn(5L);
            when(planTaskRepo.countCompletedByFamilyId(FAM)).thenReturn(5L);

            assertThat(indicator("IND-08").value()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("IND-09: Densidad de Evidencias")
    class Ind09 {

        @Test
        @DisplayName("sin misiones → valor=0")
        void sinMisiones() {
            stubAllFallbacks();

            assertThat(indicator("IND-09").value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("3 evidencias por 2 misiones → valor=100 (cap)")
        void sobreCap() {
            stubAllFallbacks();
            when(missionRepo.countCompletedByFamilyId(FAM)).thenReturn(2L);
            when(evidenceRepo.countValidatedByFamilyId(FAM)).thenReturn(3L);

            assertThat(indicator("IND-09").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("1 evidencia por 2 misiones → valor=50")
        void mitad() {
            stubAllFallbacks();
            when(missionRepo.countCompletedByFamilyId(FAM)).thenReturn(2L);
            when(evidenceRepo.countValidatedByFamilyId(FAM)).thenReturn(1L);

            assertThat(indicator("IND-09").value()).isCloseTo(50.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("IND-10: Momentum de Sprint")
    class Ind10 {

        @Test
        @DisplayName("sin sprints → valor=0")
        void sinSprints() {
            stubAllFallbacks();

            assertThat(indicator("IND-10").value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("3 sprints, 2 con retro → valor≈67")
        void dosTerciosConRetro() {
            stubAllFallbacks();
            when(sprintRepo.countByFamilyId(FAM)).thenReturn(3L);
            when(retroRepo.countByFamilyId(FAM)).thenReturn(2L);

            assertThat(indicator("IND-10").value()).isCloseTo(66.67, within(0.1));
        }

        @Test
        @DisplayName("todos los sprints con retro → valor=100")
        void todos() {
            stubAllFallbacks();
            when(sprintRepo.countByFamilyId(FAM)).thenReturn(4L);
            when(retroRepo.countByFamilyId(FAM)).thenReturn(4L);

            assertThat(indicator("IND-10").value()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("IND-11: Frecuencia de Evaluación")
    class Ind11 {

        @Test
        @DisplayName("sin evaluaciones recientes → valor=0")
        void sinEvaluaciones() {
            stubAllFallbacks();

            assertThat(indicator("IND-11").value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("1 evaluación completada en 90d → valor=50")
        void unaEvaluacion() {
            stubAllFallbacks();
            Evaluation e = completedEvaluation(LocalDateTime.now().minusDays(20));
            when(evaluationRepo.findByFamilyIdOrderByFinalizedAtAsc(FAM)).thenReturn(List.of(e));

            assertThat(indicator("IND-11").value()).isCloseTo(50.0, within(0.1));
        }

        @Test
        @DisplayName("2 evaluaciones en 90d (objetivo) → valor=100")
        void dosEvaluaciones() {
            stubAllFallbacks();
            Evaluation e1 = completedEvaluation(LocalDateTime.now().minusDays(60));
            Evaluation e2 = completedEvaluation(LocalDateTime.now().minusDays(10));
            when(evaluationRepo.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(e1, e2));

            assertThat(indicator("IND-11").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("evaluación demasiado antigua (>90d) no cuenta")
        void evaluacionAntigua() {
            stubAllFallbacks();
            Evaluation antigua = completedEvaluation(LocalDateTime.now().minusDays(100));
            when(evaluationRepo.findByFamilyIdOrderByFinalizedAtAsc(FAM)).thenReturn(List.of(antigua));

            assertThat(indicator("IND-11").value()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("IND-12: Impacto de Tareas")
    class Ind12 {

        @Test
        @DisplayName("sin tareas completadas → estimado, valor=50")
        void sinTareas() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-12");
            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("avg impacto=8 → valor=80")
        void impactoAlto() {
            stubAllFallbacks();
            when(planTaskRepo.avgImpactoIcfCompletedByFamilyId(FAM)).thenReturn(8.0);

            assertThat(indicator("IND-12").value()).isCloseTo(80.0, within(0.1));
        }

        @Test
        @DisplayName("avg impacto=10 → valor=100 (máximo)")
        void impactoMaximo() {
            stubAllFallbacks();
            when(planTaskRepo.avgImpactoIcfCompletedByFamilyId(FAM)).thenReturn(10.0);

            assertThat(indicator("IND-12").value()).isEqualTo(100.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRUPO 4 — RESILIENCIA
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IND-13: Tasa de Resolución de Crisis")
    class Ind13 {

        @Test
        @DisplayName("sin eventos → estimado, valor=50")
        void sinEventos() {
            stubAllFallbacks();

            assertThat(indicator("IND-13").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("4 eventos, 3 resueltos → valor=75")
        void tresCuartos() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "DETECTED")).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "IN_PROGRESS")).thenReturn(0L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(2L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "CLOSED")).thenReturn(1L);
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RELAPSED")).thenReturn(0L);

            assertThat(indicator("IND-13").value()).isCloseTo(75.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("IND-14: Velocidad de Recuperación")
    class Ind14 {

        @Test
        @DisplayName("sin eventos → estimado, valor=50")
        void sinEventos() {
            stubAllFallbacks();

            assertThat(indicator("IND-14").isEstimated()).isTrue();
        }

        @Test
        @DisplayName("resolución en 30 días → valor=100")
        void resolvenRapido() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(1L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM)).thenReturn(30.0);

            assertThat(indicator("IND-14").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("resolución en 180+ días → valor=0")
        void resolucionCronica() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(1L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM)).thenReturn(200.0);

            assertThat(indicator("IND-14").value()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("resolución en 120 días → valor=50 (punto medio)")
        void resolucionMedia() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(1L);
            when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM)).thenReturn(120.0);

            assertThat(indicator("IND-14").value()).isCloseTo(50.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("IND-15: Control de Recaídas")
    class Ind15 {

        @Test
        @DisplayName("sin eventos → estimado, valor=100")
        void sinEventos() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-15");
            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("0 recaídas → valor=100")
        void sinRecaidas() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(2L);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM)).thenReturn(0L);

            assertThat(indicator("IND-15").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("1 recaída en 2 eventos → valor=50")
        void unaRecaidaSobreDos() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "RESOLVED")).thenReturn(2L);
            when(criticalEventRepo.totalRelapsesByFamilyId(FAM)).thenReturn(1L);

            assertThat(indicator("IND-15").value()).isCloseTo(50.0, within(0.1));
        }
    }

    @Nested
    @DisplayName("IND-16: Carga de Crisis Activa")
    class Ind16 {

        @Test
        @DisplayName("0 activas → valor=100")
        void sinActivas() {
            stubAllFallbacks();
            when(criticalEventRepo.countActiveByFamilyId(FAM)).thenReturn(0L);

            assertThat(indicator("IND-16").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("1 activa → valor=70")
        void unaActiva() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "DETECTED")).thenReturn(1L);
            when(criticalEventRepo.countActiveByFamilyId(FAM)).thenReturn(1L);

            assertThat(indicator("IND-16").value()).isEqualTo(70.0);
        }

        @Test
        @DisplayName("≥3 activas → valor=0 (sobrecarga)")
        void tresOMas() {
            stubAllFallbacks();
            when(criticalEventRepo.countByFamilyIdAndStatus(FAM, "DETECTED")).thenReturn(3L);
            when(criticalEventRepo.countActiveByFamilyId(FAM)).thenReturn(3L);

            assertThat(indicator("IND-16").value()).isEqualTo(0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GRUPO 5 — LONGITUDINAL
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("IND-17 / IND-18: Deltas ICaF")
    class DeltasIcaf {

        @Test
        @DisplayName("IND-17: delta 6m positivo (+13) → valor por encima de 50")
        void delta6mPositivo() {
            stubAllFallbacks();
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(fullState()));
            // icafCurrent=68, icaf6mAgo=55 → delta=+13

            double val = indicator("IND-17").value();
            assertThat(val).isGreaterThan(50.0);
            assertThat(val).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("IND-17: sin datos → estimado, valor=50")
        void ind17SinDatos() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-17");
            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("IND-18: delta 12m positivo (+23) → valor por encima de 50")
        void delta12mPositivo() {
            stubAllFallbacks();
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(fullState()));
            // icafCurrent=68, icaf12mAgo=45 → delta=+23

            double val = indicator("IND-18").value();
            assertThat(val).isGreaterThan(50.0).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("delta negativo → valor por debajo de 50, mínimo 0")
        void deltaNegatvo() {
            stubAllFallbacks();
            FamilyLongitudinalState estado = new FamilyLongitudinalState();
            estado.setIcafCurrent(30.0);
            estado.setIcaf6mAgo(70.0);  // delta = -40
            estado.setIcaf12mAgo(80.0);
            estado.setIcafMadurez(2);
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(estado));

            assertThat(indicator("IND-17").value()).isLessThan(50.0).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("IND-19: Velocidad de Mejora")
    class Ind19 {

        @Test
        @DisplayName("mejora de +13 pts en 6 meses → velocidad positiva, valor>50")
        void mejoraPositiva() {
            stubAllFallbacks();
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(fullState()));

            assertThat(indicator("IND-19").value()).isGreaterThan(50.0);
        }

        @Test
        @DisplayName("sin datos → estimado")
        void sinDatos() {
            stubAllFallbacks();

            assertThat(indicator("IND-19").isEstimated()).isTrue();
        }
    }

    @Nested
    @DisplayName("IND-20: Madurez Familiar")
    class Ind20 {

        @Test
        @DisplayName("nivel 4 (Propósito) → valor=80")
        void nivel4() {
            stubAllFallbacks();
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(fullState()));

            assertThat(indicator("IND-20").value()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("nivel 5 (Legado) → valor=100")
        void nivel5() {
            stubAllFallbacks();
            FamilyLongitudinalState s = new FamilyLongitudinalState();
            s.setIcafMadurez(5);
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(indicator("IND-20").value()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("nivel 1 (Supervivencia) → valor=20")
        void nivel1() {
            stubAllFallbacks();
            FamilyLongitudinalState s = new FamilyLongitudinalState();
            s.setIcafMadurez(1);
            when(longitudinalRepo.findByFamilyId(FAM)).thenReturn(Optional.of(s));

            assertThat(indicator("IND-20").value()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("sin estado → estimado, valor=20")
        void sinEstado() {
            stubAllFallbacks();

            IndicatorResult r = indicator("IND-20");
            assertThat(r.isEstimated()).isTrue();
            assertThat(r.value()).isEqualTo(20.0);
        }
    }

    // ── Utilidades de test ────────────────────────────────────────────────────

    private IndicatorResult indicator(String id) {
        return service.computeAll(FAM).indicators().stream()
                .filter(r -> id.equals(r.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Indicador no encontrado: " + id));
    }

    private FamilySprint sprintConFechas(LocalDate start, LocalDate end) {
        FamilySprint s = new FamilySprint();
        s.setStartDate(start);
        s.setEndDate(end);
        s.setStatus("COMPLETED");
        return s;
    }

    private FamilyIcafAnswer answer(String key, int score) {
        FamilyIcafAnswer a = new FamilyIcafAnswer();
        a.setQuestionKey(key);
        a.setScore(score);
        a.setAnsweredAt(LocalDateTime.now());
        return a;
    }

    private Evaluation completedEvaluation(LocalDateTime finalizedAt) {
        Evaluation e = new Evaluation();
        e.setStatus(EvaluationStatus.COMPLETED);
        e.setFinalizedAt(finalizedAt);
        return e;
    }
}
