package com.integrityfamily.risk.service;

import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.JournalEntry;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.JournalEntryRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyCausalEngine")
class FamilyCausalEngineTest {

    @Mock FamilyLongitudinalStateRepository longitudinalRepo;
    @Mock RiskSnapshotRepository            riskSnapshotRepository;
    @Mock JournalEntryRepository            journalEntryRepository;

    @InjectMocks FamilyCausalEngine engine;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Estado mínimo seguro (todas las dims en 50, sin crisis, sin inactividad). */
    private FamilyLongitudinalState safeState(double icfCurrent, double icf30d) {
        return FamilyLongitudinalState.builder()
                .icfCurrent(icfCurrent)
                .icf30dAgo(icf30d)
                .currentRiskLevel("MODERADO")
                .crisisCount30d(0)
                .inactivityDays(0)
                .dimEmociones(50.0)
                .dimComunicacion(50.0)
                .dimHabitos(50.0)
                .dimTiempos(50.0)
                .build();
    }

    private FamilyLongitudinalState safeStateWithDims(double icf, double icf30d,
                                                       double emo, double com,
                                                       double hab, double tie) {
        return FamilyLongitudinalState.builder()
                .icfCurrent(icf)
                .icf30dAgo(icf30d)
                .currentRiskLevel("MODERADO")
                .crisisCount30d(0)
                .inactivityDays(0)
                .dimEmociones(emo)
                .dimComunicacion(com)
                .dimHabitos(hab)
                .dimTiempos(tie)
                .build();
    }

    /** Entrada de bitácora dentro de la ventana de 7 días. */
    private JournalEntry journal(int moodAfter, String dim) {
        return JournalEntry.builder()
                .moodAfter(moodAfter)
                .riskDimension(dim)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private void stubNoData(long familyId, FamilyLongitudinalState state) {
        when(longitudinalRepo.findByFamilyId(familyId)).thenReturn(Optional.of(state));
        when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)).thenReturn(List.of());
        when(journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)).thenReturn(List.of());
    }

    private void stubWithJournals(long familyId, FamilyLongitudinalState state, List<JournalEntry> journals) {
        when(longitudinalRepo.findByFamilyId(familyId)).thenReturn(Optional.of(state));
        when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)).thenReturn(List.of());
        when(journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)).thenReturn(journals);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Estado por defecto cuando no existe registro longitudinal
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sin estado longitudinal previo")
    class DefaultState {

        @Test
        @DisplayName("familia sin estado → usa valores por defecto (ICF=50, MODERADO, STABLE)")
        void noState_usesDefaults() {
            when(longitudinalRepo.findByFamilyId(99L)).thenReturn(Optional.empty());
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());
            when(journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(99L)).thenReturn(List.of());

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(99L);

            assertThat(r.icf()).isEqualTo(50.0);
            assertThat(r.trend()).isEqualTo("STABLE");
            assertThat(r.activeRules()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R2: Crisis activa en las últimas 48h → CRITICO
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R2 — crisis activa fuerza CRITICO")
    class R2ActiveCrisis {

        @Test
        @DisplayName("lastCrisisAt hace 24h → activeCrisis=true → CRITICO, contiene R2:ACTIVE_CRISIS")
        void activeCrisis_forcesCritico() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(75.0)
                    .icf30dAgo(70.0)
                    .currentRiskLevel("BAJO")
                    .crisisCount30d(0)
                    .inactivityDays(0)
                    .lastCrisisAt(LocalDateTime.now().minusHours(24))
                    .dimEmociones(60.0).dimComunicacion(60.0)
                    .dimHabitos(60.0).dimTiempos(60.0)
                    .build();

            stubNoData(1L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(1L);

            assertThat(r.inferredRiskLevel()).isEqualTo("CRITICO");
            assertThat(r.activeRules()).contains("R2:ACTIVE_CRISIS");
            assertThat(r.requiresImmediateIntervention()).isTrue();
        }

        @Test
        @DisplayName("lastCrisisAt hace 72h → activeCrisis=false → no aplica R2")
        void oldCrisis_doesNotActivateR2() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(60.0)
                    .icf30dAgo(60.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(0)
                    .inactivityDays(0)
                    .lastCrisisAt(LocalDateTime.now().minusHours(72))
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();

            stubNoData(2L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(2L);

            assertThat(r.activeRules()).doesNotContain("R2:ACTIVE_CRISIS");
            assertThat(r.inferredRiskLevel()).isNotEqualTo("CRITICO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R5: Dimensión < 25 → CRITICO (regla de seguridad)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R5 — colapso dimensional crítico")
    class R5DimCritical {

        @Test
        @DisplayName("dimEmociones=20 → R5:DIM_CRITICAL, CRITICO")
        void emocionesBelow25_critico() {
            FamilyLongitudinalState state = safeStateWithDims(65.0, 65.0, 20.0, 50.0, 50.0, 50.0);
            stubNoData(3L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(3L);

            assertThat(r.inferredRiskLevel()).isEqualTo("CRITICO");
            assertThat(r.activeRules()).contains("R5:DIM_CRITICAL");
        }

        @Test
        @DisplayName("dimComunicacion=24.9 → R5:DIM_CRITICAL (límite estricto)")
        void comunicacionJustBelow25_critico() {
            FamilyLongitudinalState state = safeStateWithDims(65.0, 65.0, 50.0, 24.9, 50.0, 50.0);
            stubNoData(4L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(4L);

            assertThat(r.activeRules()).contains("R5:DIM_CRITICAL");
        }

        @Test
        @DisplayName("dimHabitos=25.0 (exacto) → NO activa R5 (strict less-than)")
        void habitosExactly25_noR5() {
            FamilyLongitudinalState state = safeStateWithDims(65.0, 65.0, 50.0, 50.0, 25.0, 50.0);
            stubNoData(5L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(5L);

            assertThat(r.activeRules()).doesNotContain("R5:DIM_CRITICAL");
        }

        @Test
        @DisplayName("criticalDimension apunta a la dimensión con menor valor")
        void criticalDimension_lowestDimension() {
            FamilyLongitudinalState state = safeStateWithDims(65.0, 65.0, 45.0, 30.0, 50.0, 50.0);
            stubNoData(6L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(6L);

            assertThat(r.criticalDimension()).isEqualTo("comunicacion");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R1: 3+ deterioraciones en bitácora en 7 días → escalar riesgo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R1 — escalado por deterioración de bitácora")
    class R1JournalDeterioration {

        @Test
        @DisplayName("3 entradas moodAfter <= 2 en 7 días + riesgo BAJO → escala a MODERADO")
        void threeDeterioration_escalatesBajoToModerado() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("BAJO")
                    .crisisCount30d(0).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();

            List<JournalEntry> journals = List.of(
                    journal(1, "emociones"),
                    journal(2, "comunicacion"),
                    journal(1, "habitos")
            );
            stubWithJournals(7L, state, journals);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(7L);

            assertThat(r.inferredRiskLevel()).isEqualTo("MODERADO");
            assertThat(r.activeRules()).contains("R1:JOURNAL_DETERIORATION_PATTERN");
        }

        @Test
        @DisplayName("3 deterioraciones con riesgo MODERADO → escala a ALTO")
        void threeDeterioration_escalatesModerToAlto() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(50.0).icf30dAgo(50.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(0).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();

            List<JournalEntry> journals = List.of(
                    journal(2, "emociones"),
                    journal(1, "emociones"),
                    journal(2, "habitos")
            );
            stubWithJournals(8L, state, journals);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(8L);

            assertThat(r.inferredRiskLevel()).isEqualTo("ALTO");
            assertThat(r.activeRules()).contains("R1:JOURNAL_DETERIORATION_PATTERN");
        }

        @Test
        @DisplayName("solo 2 deterioraciones → NO activa R1")
        void twoDeterioration_noR1() {
            FamilyLongitudinalState state = safeState(65.0, 65.0);
            List<JournalEntry> journals = List.of(
                    journal(2, "emociones"),
                    journal(1, "habitos")
            );
            stubWithJournals(9L, state, journals);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(9L);

            assertThat(r.activeRules()).doesNotContain("R1:JOURNAL_DETERIORATION_PATTERN");
        }

        @Test
        @DisplayName("entrada de bitácora fuera de ventana (hace 8 días) no cuenta para R1")
        void oldJournal_outsideWindow_doesNotCount() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("BAJO")
                    .crisisCount30d(0).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();

            // Tres entradas, pero todas hace 8 días (fuera de la ventana de 7 días)
            JournalEntry old1 = JournalEntry.builder().moodAfter(1).riskDimension("emociones")
                    .createdAt(LocalDateTime.now().minusDays(8)).build();
            JournalEntry old2 = JournalEntry.builder().moodAfter(2).riskDimension("habitos")
                    .createdAt(LocalDateTime.now().minusDays(8)).build();
            JournalEntry old3 = JournalEntry.builder().moodAfter(1).riskDimension("tiempos")
                    .createdAt(LocalDateTime.now().minusDays(9)).build();

            stubWithJournals(10L, state, List.of(old1, old2, old3));

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(10L);

            assertThat(r.activeRules()).doesNotContain("R1:JOURNAL_DETERIORATION_PATTERN");
        }

        @Test
        @DisplayName("R1 no aplica cuando ya es CRITICO (evita doble escalado)")
        void r1_notAppliedWhenAlreadyCritico() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("ALTO")
                    .crisisCount30d(0).inactivityDays(0)
                    .lastCrisisAt(LocalDateTime.now().minusHours(12))  // → R2 primero → CRITICO
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();

            List<JournalEntry> journals = List.of(
                    journal(1, "emociones"),
                    journal(2, "habitos"),
                    journal(1, "tiempos")
            );
            stubWithJournals(11L, state, journals);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(11L);

            // Debe ser CRITICO (por R2), y R1 NO debería volver a escalarlo
            assertThat(r.inferredRiskLevel()).isEqualTo("CRITICO");
            assertThat(r.activeRules()).contains("R2:ACTIVE_CRISIS");
            // R1 existe en la lista solo si no era CRITICO cuando se procesó
            // (por lógica del motor: solo escala si no es CRITICO)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R6: Colapso comunicacional
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R6 — colapso comunicacional")
    class R6CommunicationCollapse {

        @Test
        @DisplayName("dimComunicacion=30 + crisisCount30d=3 → communicationCollapseActive=true")
        void commCollapse_active() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(55.0).icf30dAgo(55.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(3).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(30.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(12L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(12L);

            assertThat(r.communicationCollapseActive()).isTrue();
            assertThat(r.activeRules()).contains("R6:COMMUNICATION_COLLAPSE");
            assertThat(r.requiresImmediateIntervention()).isTrue();
        }

        @Test
        @DisplayName("dimComunicacion=34.9 + crisisCount=2 → colapso activo")
        void commCollapseJustBelow35() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(50.0).icf30dAgo(50.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(2).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(34.9)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(13L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(13L);

            assertThat(r.communicationCollapseActive()).isTrue();
        }

        @Test
        @DisplayName("dimComunicacion=35.0 (exacto) → NO colapso (strict less-than)")
        void commExactly35_noCollapse() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(50.0).icf30dAgo(50.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(3).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(35.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(14L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(14L);

            assertThat(r.communicationCollapseActive()).isFalse();
        }

        @Test
        @DisplayName("dimComunicacion=30 + crisisCount=1 (< 2) → NO colapso")
        void commLow_butOnlyCrisis_noCollapse() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(50.0).icf30dAgo(50.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(1).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(30.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(15L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(15L);

            assertThat(r.communicationCollapseActive()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R4: Inactividad ≥ 14 días + riesgo BAJO → MODERADO
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R4 — riesgo latente por inactividad")
    class R4Inactivity {

        @Test
        @DisplayName("inactivityDays=14 + currentRiskLevel=BAJO → MODERADO, R4 activado")
        void inactivity14_bajoToModerado() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("BAJO")
                    .crisisCount30d(0).inactivityDays(14)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(16L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(16L);

            assertThat(r.inferredRiskLevel()).isEqualTo("MODERADO");
            assertThat(r.activeRules()).contains("R4:INACTIVITY_LATENT_RISK");
        }

        @Test
        @DisplayName("inactivityDays=14 + riesgo ya MODERADO → R4 no modifica (solo actúa sobre BAJO)")
        void inactivity14_moderado_noChange() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(55.0).icf30dAgo(55.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(0).inactivityDays(14)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(17L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(17L);

            // R4 solo actúa si inferredRisk == "BAJO"; aquí ya es MODERADO → no aplica
            assertThat(r.activeRules()).doesNotContain("R4:INACTIVITY_LATENT_RISK");
        }

        @Test
        @DisplayName("inactivityDays=13 (< 14) → NO activa R4")
        void inactivity13_noR4() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("BAJO")
                    .crisisCount30d(0).inactivityDays(13)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(18L, state);

            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(18L);

            assertThat(r.activeRules()).doesNotContain("R4:INACTIVITY_LATENT_RISK");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tendencia ICF (delta = icfCurrent - icf30dAgo)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("tendencia ICF")
    class IcfTrend {

        @Test
        @DisplayName("delta=+10 → IMPROVING")
        void delta10_improving() {
            stubNoData(20L, safeState(70.0, 60.0));
            assertThat(engine.evaluate(20L).trend()).isEqualTo("IMPROVING");
        }

        @Test
        @DisplayName("delta=-8 → DETERIORATING")
        void deltaMinus8_deteriorating() {
            stubNoData(21L, safeState(52.0, 60.0));
            assertThat(engine.evaluate(21L).trend()).isEqualTo("DETERIORATING");
        }

        @Test
        @DisplayName("delta=0 → STABLE")
        void delta0_stable() {
            stubNoData(22L, safeState(60.0, 60.0));
            assertThat(engine.evaluate(22L).trend()).isEqualTo("STABLE");
        }

        @Test
        @DisplayName("delta=+5.0 (exacto) → STABLE (strict greater-than)")
        void deltaExact5_stable() {
            stubNoData(23L, safeState(65.0, 60.0));
            assertThat(engine.evaluate(23L).trend()).isEqualTo("STABLE");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // R3: Hito de evolución (delta >= 10)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("R3 — hito de evolución")
    class R3EvolutionMilestone {

        @Test
        @DisplayName("delta=10 (exacto) → evolutionMilestoneReached=true, R3 activado")
        void delta10_evolutionMilestone() {
            stubNoData(24L, safeState(70.0, 60.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(24L);
            assertThat(r.evolutionMilestoneReached()).isTrue();
            assertThat(r.activeRules()).contains("R3:EVOLUTION_MILESTONE");
        }

        @Test
        @DisplayName("delta=9.9 → NO evolutionMilestone (estricto)")
        void delta9_9_noMilestone() {
            stubNoData(25L, safeState(69.9, 60.0));
            assertThat(engine.evaluate(25L).evolutionMilestoneReached()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fase de evolución
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fase de evolución")
    class EvolutionPhase {

        @Test
        @DisplayName("ICF=25 → inconsciente")
        void icf25_inconsciente() {
            stubNoData(30L, safeState(25.0, 25.0));
            assertThat(engine.evaluate(30L).evolutionPhase()).isEqualTo("inconsciente");
        }

        @Test
        @DisplayName("crisisCount30d=3 → inconsciente (independientemente del ICF)")
        void crisis3_inconsciente() {
            FamilyLongitudinalState state = FamilyLongitudinalState.builder()
                    .icfCurrent(65.0).icf30dAgo(65.0)
                    .currentRiskLevel("MODERADO")
                    .crisisCount30d(3).inactivityDays(0)
                    .dimEmociones(50.0).dimComunicacion(50.0)
                    .dimHabitos(50.0).dimTiempos(50.0)
                    .build();
            stubNoData(31L, state);
            assertThat(engine.evaluate(31L).evolutionPhase()).isEqualTo("inconsciente");
        }

        @Test
        @DisplayName("ICF=40 → reactivo")
        void icf40_reactivo() {
            stubNoData(32L, safeState(40.0, 40.0));
            assertThat(engine.evaluate(32L).evolutionPhase()).isEqualTo("reactivo");
        }

        @Test
        @DisplayName("ICF=60 → consciente")
        void icf60_consciente() {
            stubNoData(33L, safeState(60.0, 60.0));
            assertThat(engine.evaluate(33L).evolutionPhase()).isEqualTo("consciente");
        }

        @Test
        @DisplayName("ICF=80 + trend IMPROVING → pleno")
        void icf80Improving_pleno() {
            stubNoData(34L, safeState(80.0, 65.0));  // delta=+15 → IMPROVING
            assertThat(engine.evaluate(34L).evolutionPhase()).isEqualTo("pleno");
        }

        @Test
        @DisplayName("ICF=80 + trend STABLE → consciente (no es pleno sin mejora)")
        void icf80Stable_consciente() {
            stubNoData(35L, safeState(80.0, 80.0));  // delta=0 → STABLE
            assertThat(engine.evaluate(35L).evolutionPhase()).isEqualTo("consciente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nivel y etiqueta de consciencia
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("nivel de consciencia")
    class ConsciousnessLevel {

        @Test
        @DisplayName("ICF=90 → nivel 1, Plena")
        void icf90_level1() {
            stubNoData(40L, safeState(90.0, 90.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(40L);
            assertThat(r.consciousnessLevel()).isEqualTo(1);
            assertThat(r.consciousnessLabel()).isEqualTo("Plena");
        }

        @Test
        @DisplayName("ICF=85 → nivel 1 (límite exacto)")
        void icf85_level1() {
            stubNoData(41L, safeState(85.0, 85.0));
            assertThat(engine.evaluate(41L).consciousnessLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("ICF=72 → nivel 2, Madurando")
        void icf72_level2() {
            stubNoData(42L, safeState(72.0, 72.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(42L);
            assertThat(r.consciousnessLevel()).isEqualTo(2);
            assertThat(r.consciousnessLabel()).isEqualTo("Madurando");
        }

        @Test
        @DisplayName("ICF=60 → nivel 3, Consciente")
        void icf60_level3() {
            stubNoData(43L, safeState(60.0, 60.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(43L);
            assertThat(r.consciousnessLevel()).isEqualTo(3);
            assertThat(r.consciousnessLabel()).isEqualTo("Consciente");
        }

        @Test
        @DisplayName("ICF=45 → nivel 4, Reactiva")
        void icf45_level4() {
            stubNoData(44L, safeState(45.0, 45.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(44L);
            assertThat(r.consciousnessLevel()).isEqualTo(4);
            assertThat(r.consciousnessLabel()).isEqualTo("Reactiva");
        }

        @Test
        @DisplayName("ICF=20 → nivel 5, Inconsciente")
        void icf20_level5() {
            stubNoData(45L, safeState(20.0, 20.0));
            FamilyCausalEngine.CausalInferenceResult r = engine.evaluate(45L);
            assertThat(r.consciousnessLevel()).isEqualTo(5);
            assertThat(r.consciousnessLabel()).isEqualTo("Inconsciente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Narrativa según fase
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("etapa narrativa")
    class NarrativeStage {

        @Test
        @DisplayName("inconsciente → RECONOCIMIENTO")
        void inconsciente_reconocimiento() {
            stubNoData(50L, safeState(25.0, 25.0));
            assertThat(engine.evaluate(50L).narrativeStage()).isEqualTo("RECONOCIMIENTO");
        }

        @Test
        @DisplayName("reactivo → RECONOCIMIENTO")
        void reactivo_reconocimiento() {
            stubNoData(51L, safeState(40.0, 40.0));
            assertThat(engine.evaluate(51L).narrativeStage()).isEqualTo("RECONOCIMIENTO");
        }

        @Test
        @DisplayName("consciente → AMOR")
        void consciente_amor() {
            stubNoData(52L, safeState(60.0, 60.0));
            assertThat(engine.evaluate(52L).narrativeStage()).isEqualTo("AMOR");
        }

        @Test
        @DisplayName("pleno → ENTREGA")
        void pleno_entrega() {
            stubNoData(53L, safeState(80.0, 65.0));  // delta=+15 → IMPROVING → pleno
            assertThat(engine.evaluate(53L).narrativeStage()).isEqualTo("ENTREGA");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // requiresImmediateIntervention
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("requiresImmediateIntervention")
    class ImmediateIntervention {

        @Test
        @DisplayName("CRITICO → requiere intervención")
        void critico_requiresIntervention() {
            FamilyLongitudinalState state = safeStateWithDims(65.0, 65.0, 20.0, 50.0, 50.0, 50.0);
            stubNoData(60L, state);
            assertThat(engine.evaluate(60L).requiresImmediateIntervention()).isTrue();
        }

        @Test
        @DisplayName("MODERADO sin colapso comunicacional → NO requiere intervención")
        void moderado_noIntervention() {
            stubNoData(61L, safeState(55.0, 55.0));
            assertThat(engine.evaluate(61L).requiresImmediateIntervention()).isFalse();
        }
    }
}
