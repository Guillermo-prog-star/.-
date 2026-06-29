package com.integrityfamily.capital.service;

import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IcafResilienciaEngine — Unit Tests")
class IcafResilienciaEngineTest {

    @Mock FamilyCriticalEventRepository criticalEventRepo;

    @InjectMocks IcafResilienciaEngine engine;

    private static final Long FAM_ID = 1L;

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Sin eventos: totalEvents = 0 → fallback path */
    private void stubNoEvents() {
        when(criticalEventRepo.countByFamilyIdAndStatus(eq(FAM_ID), anyString())).thenReturn(0L);
    }

    /**
     * Stub completo con control total sobre cada contador.
     * total = detected + inProgress + resolved + closed + relapsed
     */
    private void stubRaw(long detected, long inProgress, long resolved,
                         long closed, long relapsed,
                         long active, double avgDays, long relapseCount) {
        when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "DETECTED")).thenReturn(detected);
        when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "IN_PROGRESS")).thenReturn(inProgress);
        when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RESOLVED")).thenReturn(resolved);
        when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "CLOSED")).thenReturn(closed);
        when(criticalEventRepo.countByFamilyIdAndStatus(FAM_ID, "RELAPSED")).thenReturn(relapsed);
        when(criticalEventRepo.countActiveByFamilyId(FAM_ID)).thenReturn(active);
        when(criticalEventRepo.avgDaysToResolutionByFamilyId(FAM_ID)).thenReturn(avgDays);
        when(criticalEventRepo.totalRelapsesByFamilyId(FAM_ID)).thenReturn(relapseCount);
    }

    /** Familia perfecta: todos resueltos, rápido, sin recaídas, sin activos */
    private void stubPerfect(long resolved) {
        stubRaw(0, 0, resolved, 0, 0, 0, 30.0, 0);
    }

    private FamilyLongitudinalState ltsWith(String riskTrend, Integer crisisCount, Integer improvements) {
        FamilyLongitudinalState lts = new FamilyLongitudinalState();
        lts.setRiskTrend(riskTrend);
        lts.setCrisisCount30d(crisisCount);
        lts.setConsecutiveImprovements(improvements);
        return lts;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fallback — sin eventos críticos registrados")
    class FallbackPath {

        @Test
        @DisplayName("devuelve 50 cuando no hay LTS ni eventos")
        void returns50WithNullLts() {
            stubNoEvents();
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(50.0);
        }

        @Test
        @DisplayName("fallback IMPROVING sin crisis = 70")
        void improvingNoCrisis() {
            stubNoEvents();
            assertThat(engine.compute(FAM_ID, ltsWith("IMPROVING", 0, 0))).isEqualTo(70.0);
        }

        @Test
        @DisplayName("fallback no-IMPROVING sin crisis = 55")
        void notImprovingNoCrisis() {
            stubNoEvents();
            assertThat(engine.compute(FAM_ID, ltsWith("DETERIORATING", 0, 0))).isEqualTo(55.0);
        }

        @Test
        @DisplayName("cada crisis reciente resta 10 puntos desde la base")
        void eachCrisisSubtracts10() {
            stubNoEvents();
            // IMPROVING base 70, -2*10 = 50
            assertThat(engine.compute(FAM_ID, ltsWith("IMPROVING", 2, 0))).isEqualTo(50.0);
        }

        @Test
        @DisplayName("mejoras consecutivas suman hasta máximo 15 puntos")
        void consecutiveImprovementsCappedAt15() {
            stubNoEvents();
            // IMPROVING 70 + min(10*3, 15) = 85
            assertThat(engine.compute(FAM_ID, ltsWith("IMPROVING", 0, 10))).isEqualTo(85.0);
        }

        @Test
        @DisplayName("score nunca supera 100 en el fallback")
        void scoreClampedAt100() {
            stubNoEvents();
            assertThat(engine.compute(FAM_ID, ltsWith("IMPROVING", 0, 100)))
                    .isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("score nunca es negativo en el fallback")
        void scoreNeverNegative() {
            stubNoEvents();
            assertThat(engine.compute(FAM_ID, ltsWith("DETERIORATING", 20, 0)))
                    .isGreaterThanOrEqualTo(0.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("componente A — tasa de resolución (peso 40%)")
    class ComponentA {

        @Test
        @DisplayName("100% resolución → A=100 → score 100")
        void allResolvedGivesA100() {
            // total=5, resolved=5 → A=100, B=100, C=100, D=100 → score=100
            stubPerfect(5);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("0% resolución → A=0 — penaliza con 40 puntos menos")
        void noneResolvedLowersScore() {
            // total=1 DETECTED, resolved=0, active=1, avgDays=0, relapses=0
            // A=0*0.4 + B=50*0.3 + C=100*0.2 + D=70*0.1 = 0+15+20+7 = 42
            stubRaw(1, 0, 0, 0, 0, 1, 0.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(42.0);
        }

        @Test
        @DisplayName("50% resolución → A contribuye 20 puntos al score final")
        void halfResolvedContributes20Points() {
            // total=2 (1 DETECTED + 1 RESOLVED), resolved=1
            // A=50, B=100 (avgDays=30), C=100, D=100
            // 50*0.4 + 100*0.3 + 100*0.2 + 100*0.1 = 20+30+20+10 = 80
            stubRaw(1, 0, 1, 0, 0, 0, 30.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(80.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("componente B — velocidad de recuperación (peso 30%)")
    class ComponentB {

        @Test
        @DisplayName("recuperación en 30 días (≤60) → B=100 → score=100")
        void under60DaysGivesB100() {
            stubPerfect(2);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("recuperación en exactamente 60 días → B=100")
        void exactly60DaysGivesB100() {
            stubRaw(0, 0, 2, 0, 0, 0, 60.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("recuperación en 180+ días → B=0 — score=70 (pierde 30%)")
        void over180DaysGivesB0() {
            // A=100, B=0, C=100, D=100
            // 100*0.4 + 0*0.3 + 100*0.2 + 100*0.1 = 40+0+20+10 = 70
            stubRaw(0, 0, 2, 0, 0, 0, 200.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(70.0);
        }

        @Test
        @DisplayName("sin resoluciones aún (avgDays=0) → B=50 (neutral)")
        void noResolutionsYetIsNeutral() {
            // total=1 DETECTED, resolved=0, active=1, avgDays=0, relapses=0
            // A=0, B=50, C=100, D=70 → 0+15+20+7 = 42
            stubRaw(1, 0, 0, 0, 0, 1, 0.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(42.0);
        }

        @Test
        @DisplayName("recuperación a 120 días → B=50 (punto medio del rango)")
        void midpointRecovery() {
            // B = (180-120)/(180-60)*100 = 60/120*100 = 50
            // A=100, B=50, C=100, D=100 → 40+15+20+10 = 85
            stubRaw(0, 0, 2, 0, 0, 0, 120.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(85.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("componente C — control de recaídas (peso 20%)")
    class ComponentC {

        @Test
        @DisplayName("sin recaídas → C=100 → no penaliza")
        void noRelapsesGivesC100() {
            stubPerfect(2);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("1 recaída en 2 eventos → C=50 → score=90")
        void oneRelapseInTwoEventsGivesC50() {
            // total = RESOLVED(1) + RELAPSED(1) = 2
            // A = 1/2 * 100 = 50; B=100 (avgDays=30); C = (1-0.5)*100=50; D=100
            // 50*0.4 + 100*0.3 + 50*0.2 + 100*0.1 = 20+30+10+10 = 70
            stubRaw(0, 0, 1, 0, 1, 0, 30.0, 1);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(70.0);
        }

        @Test
        @DisplayName("recaídas ≥ total de eventos → C=0 — penaliza con 20 puntos menos")
        void maxRelapsesGivesC0() {
            // total=2 (RESOLVED(1)+RELAPSED(1)), relapses=2 → ratio=min(2/2,1)=1 → C=0
            // A = 1/2*100=50; B=100; C=0; D=100
            // 50*0.4 + 100*0.3 + 0*0.2 + 100*0.1 = 20+30+0+10 = 60
            stubRaw(0, 0, 1, 0, 1, 0, 30.0, 2);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(60.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("componente D — carga activa inversa (peso 10%)")
    class ComponentD {

        @Test
        @DisplayName("0 activos → D=100 → contribuye 10 puntos completos")
        void zeroActiveGivesD100() {
            // A=100, B=100, C=100, D=100 → 100
            stubPerfect(2);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("1 activo → D=70 → pierde 3 puntos respecto a 0 activos")
        void oneActiveGivesD70() {
            // A=100, B=100, C=100, D=70
            // 100*0.4 + 100*0.3 + 100*0.2 + 70*0.1 = 40+30+20+7 = 97
            stubRaw(0, 0, 2, 0, 0, 1, 30.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(97.0);
        }

        @Test
        @DisplayName("2 activos → D=40 → pierde 6 puntos respecto a 0 activos")
        void twoActiveGivesD40() {
            // A=100, B=100, C=100, D=40
            // 100*0.4 + 100*0.3 + 100*0.2 + 40*0.1 = 40+30+20+4 = 94
            stubRaw(0, 0, 2, 0, 0, 2, 30.0, 0);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(94.0);
        }

        @Test
        @DisplayName("≥3 activos → D=0 → pierde 10 puntos respecto a 0 activos")
        void threeOrMoreActiveGivesD0() {
            // A=100, B=100, C=100, D=0
            // 100*0.4 + 100*0.3 + 100*0.2 + 0*0.1 = 40+30+20+0 = 90
            stubRaw(0, 0, 2, 0, 0, 3, 30.0, 0);
            double threeActive = engine.compute(FAM_ID, null);
            assertThat(threeActive).isEqualTo(90.0);

            reset(criticalEventRepo);
            stubPerfect(2);
            double zeroActive = engine.compute(FAM_ID, null);
            assertThat(zeroActive - threeActive).isEqualTo(10.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("score general — invariantes")
    class ScoreInvariants {

        @Test
        @DisplayName("familia perfecta → exactamente 100")
        void perfectFamilyGives100() {
            stubPerfect(5);
            assertThat(engine.compute(FAM_ID, null)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("score siempre entre 0 y 100")
        void scoreBoundedBetween0And100() {
            // Peor caso posible
            stubRaw(3, 0, 0, 0, 3, 3, 300.0, 10);
            double score = engine.compute(FAM_ID, null);
            assertThat(score).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("resultado redondeado a 2 decimales como máximo")
        void resultRoundedTo2Decimals() {
            // A = 1/3 * 100 ≈ 33.33... → debe quedar redondeado
            stubRaw(1, 0, 1, 0, 1, 1, 120.0, 1);
            double score = engine.compute(FAM_ID, null);
            String s = String.valueOf(score);
            int dotIdx = s.indexOf('.');
            int decimals = dotIdx < 0 ? 0 : s.length() - dotIdx - 1;
            assertThat(decimals).isLessThanOrEqualTo(2);
        }
    }
}
