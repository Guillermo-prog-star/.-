package com.integrityfamily.twin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.twin.domain.FamilyTwinProfile;
import com.integrityfamily.twin.dto.DigitalTwinDto;
import com.integrityfamily.twin.repository.FamilyPredictionRepository;
import com.integrityfamily.twin.repository.FamilyTwinRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DigitalTwinService")
class DigitalTwinServiceTest {

    @Mock FamilyTwinRepository           twinRepository;
    @Mock FamilyPredictionRepository     predictionRepository;
    @Mock FamilyRepository               familyRepository;
    @Mock FamilyGratitudeEntryRepository gratitudeRepository;
    @Mock TaskEvidenceRepository         evidenceRepository;
    @Mock FamilyLogbookRepository        logbookRepository;
    @Mock CriticalDayRepository          crisisRepository;
    @Mock FamilySprintRepository         sprintRepository;
    @Mock FamilyLongitudinalStateRepository ltsRepository;
    @Mock FamilyContextEngine            contextEngine;
    @Mock FamilyDnaService               dnaService;
    @Mock AiProvider                     aiProvider;
    @Spy  ObjectMapper                   objectMapper = new ObjectMapper();

    @InjectMocks DigitalTwinService service;

    private final Family TEST_FAMILY = Family.builder()
            .id(1L).name("Familia Test").currentMilestone("W1").build();

    /** Stub mínimo para que compute() no lance NPE. */
    @BeforeEach
    void stubDefaults() {
        lenient().when(familyRepository.findById(anyLong())).thenReturn(Optional.of(TEST_FAMILY));
        lenient().when(ltsRepository.findByFamilyId(anyLong())).thenReturn(Optional.empty());
        lenient().when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(evidenceRepository.findByFamilyId(anyLong())).thenReturn(List.of());
        lenient().when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        lenient().when(twinRepository.findByFamilyId(anyLong())).thenReturn(Optional.empty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FamilyLongitudinalState lts(int detCount, int improvements, int crisis30d,
                                         boolean collapse, Double dimComm,
                                         LocalDateTime lastAssessment) {
        return FamilyLongitudinalState.builder()
                .consecutiveDeteriorations(detCount)
                .consecutiveImprovements(improvements)
                .crisisCount30d(crisis30d)
                .communicationCollapseActive(collapse)
                .dimComunicacion(dimComm)
                .lastAssessmentAt(lastAssessment)
                .inactivityDays(0)
                .build();
    }

    private FamilyGratitudeEntry recentGratitude() {
        return FamilyGratitudeEntry.builder()
                .createdAt(LocalDateTime.now().minusDays(1)).build();
    }

    private TaskEvidence recentEvidence() {
        return TaskEvidence.builder()
                .createdAt(LocalDateTime.now().minusDays(1)).build();
    }

    private CriticalDay crisis(LocalDateTime at) {
        CriticalDay c = new CriticalDay();
        c.setCreatedAt(at);
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTwin
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTwin")
    class GetTwin {

        @Test
        @DisplayName("sin gemelo en DB → Optional.empty()")
        void noTwin_returnsEmpty() {
            when(twinRepository.findByFamilyId(1L)).thenReturn(Optional.empty());

            assertThat(service.getTwin(1L)).isEmpty();
        }

        @Test
        @DisplayName("gemelo existente → DTO con nombre de familia")
        void existingTwin_returnsDto() {
            FamilyTwinProfile twin = FamilyTwinProfile.builder()
                    .familyId(1L).resilienceIndex(75.0).build();
            when(twinRepository.findByFamilyId(1L)).thenReturn(Optional.of(twin));
            when(familyRepository.findById(1L)).thenReturn(Optional.of(TEST_FAMILY));
            when(predictionRepository.findByFamilyIdAndStatusOrderByConfidenceDesc(1L, "ACTIVE"))
                    .thenReturn(List.of());

            Optional<DigitalTwinDto> dto = service.getTwin(1L);

            assertThat(dto).isPresent();
            assertThat(dto.get().familyName()).isEqualTo("Familia Test");
            assertThat(dto.get().resilienceIndex()).isEqualTo(75.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildTwinContextBlock
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildTwinContextBlock")
    class ContextBlock {

        @Test
        @DisplayName("sin gemelo → null")
        void noTwin_null() {
            when(twinRepository.findByFamilyId(10L)).thenReturn(Optional.empty());

            assertThat(service.buildTwinContextBlock(10L)).isNull();
        }

        @Test
        @DisplayName("gemelo con todos los campos → bloque de texto con secciones")
        void fullTwin_buildsBlock() {
            FamilyTwinProfile twin = FamilyTwinProfile.builder()
                    .familyId(10L)
                    .behavioralSignature("Firma de prueba.")
                    .dominantStrength("Resiliencia")
                    .dominantVulnerability("Comunicación bajo presión")
                    .resilienceIndex(70.0)
                    .build();
            when(twinRepository.findByFamilyId(10L)).thenReturn(Optional.of(twin));

            String block = service.buildTwinContextBlock(10L);

            assertThat(block).isNotNull();
            assertThat(block).contains("Gemelo Digital");
            assertThat(block).contains("Firma de prueba");
            assertThat(block).contains("Resiliencia");
            assertThat(block).contains("70");
        }

        @Test
        @DisplayName("gemelo sin firma ni fortaleza → bloque solo con resiliencia")
        void minimalTwin_resilienceOnly() {
            FamilyTwinProfile twin = FamilyTwinProfile.builder()
                    .familyId(10L)
                    .resilienceIndex(50.0)
                    .build();
            when(twinRepository.findByFamilyId(10L)).thenReturn(Optional.of(twin));

            String block = service.buildTwinContextBlock(10L);

            assertThat(block).isNotNull().contains("50");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute() — predicciones deterministas
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("predicciones deterministas")
    class Predictions {

        @Test
        @DisplayName("consecutiveDeteriorations=2 → TENSION_RISK generado")
        void tensionRisk_byDeteriorations() {
            when(ltsRepository.findByFamilyId(1L))
                    .thenReturn(Optional.of(lts(2, 0, 0, false, 50.0, null)));

            DigitalTwinDto dto = service.compute(1L);

            boolean hasTension = dto.activePredictions().stream()
                    .anyMatch(p -> "TENSION_RISK".equals(p.predictionType()));
            assertThat(hasTension).isTrue();
        }

        @Test
        @DisplayName("crisisCount30d=1 → TENSION_RISK generado (aunque det=0)")
        void tensionRisk_byCrisis30d() {
            when(ltsRepository.findByFamilyId(1L))
                    .thenReturn(Optional.of(lts(0, 0, 1, false, 50.0, null)));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "TENSION_RISK".equals(p.predictionType()));
        }

        @Test
        @DisplayName("consecutiveImprovements=2 → GROWTH_OPPORTUNITY generado")
        void growthOpportunity() {
            when(ltsRepository.findByFamilyId(1L))
                    .thenReturn(Optional.of(lts(0, 2, 0, false, 50.0, null)));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "GROWTH_OPPORTUNITY".equals(p.predictionType()));
        }

        @Test
        @DisplayName("dimComunicacion=30 (< 40) sin colapso → COMMUNICATION_ALERT generado")
        void communicationAlert() {
            when(ltsRepository.findByFamilyId(1L))
                    .thenReturn(Optional.of(lts(0, 0, 0, false, 30.0, null)));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "COMMUNICATION_ALERT".equals(p.predictionType()));
        }

        @Test
        @DisplayName("dimComunicacion=30 CON colapso → NO COMMUNICATION_ALERT (solo si !commAlert)")
        void communicationAlert_notWhenCollapse() {
            when(ltsRepository.findByFamilyId(1L))
                    .thenReturn(Optional.of(lts(0, 0, 0, true, 30.0, null)));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions())
                    .noneMatch(p -> "COMMUNICATION_ALERT".equals(p.predictionType()));
        }

        @Test
        @DisplayName("5 gratitudes recientes → RITUAL_READINESS generado")
        void ritualReadiness_fiveGratitudes() {
            List<FamilyGratitudeEntry> grats = Collections.nCopies(5, recentGratitude());
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(grats);

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "RITUAL_READINESS".equals(p.predictionType()));
        }

        @Test
        @DisplayName("3 gratitudes + 2 evidencias recientes → RITUAL_READINESS generado (total=5)")
        void ritualReadiness_mixedEvents() {
            List<FamilyGratitudeEntry> grats = Collections.nCopies(3, recentGratitude());
            List<TaskEvidence> evids = Collections.nCopies(2, recentEvidence());
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(grats);
            when(evidenceRepository.findByFamilyId(1L)).thenReturn(evids);

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "RITUAL_READINESS".equals(p.predictionType()));
        }

        @Test
        @DisplayName("4 gratitudes recientes → NO RITUAL_READINESS (total < 5)")
        void noRitualReadiness_onlyFour() {
            List<FamilyGratitudeEntry> grats = Collections.nCopies(4, recentGratitude());
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(grats);

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions())
                    .noneMatch(p -> "RITUAL_READINESS".equals(p.predictionType()));
        }

        @Test
        @DisplayName("lastAssessmentAt hace 85 días → EVALUATION_DUE generado")
        void evaluationDue_85daysAgo() {
            FamilyLongitudinalState ltsVal = lts(0, 0, 0, false, 50.0,
                    LocalDateTime.now().minusDays(85));
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).anyMatch(p -> "EVALUATION_DUE".equals(p.predictionType()));
        }

        @Test
        @DisplayName("lastAssessmentAt hace 79 días → NO EVALUATION_DUE (< 80)")
        void noEvaluationDue_79daysAgo() {
            FamilyLongitudinalState ltsVal = lts(0, 0, 0, false, 50.0,
                    LocalDateTime.now().minusDays(79));
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions())
                    .noneMatch(p -> "EVALUATION_DUE".equals(p.predictionType()));
        }

        @Test
        @DisplayName("sin LTS ni eventos → sin predicciones")
        void noLts_noEvents_noPredictions() {
            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.activePredictions()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute() — patrones conductuales
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("patrones conductuales detectados")
    class Patterns {

        @Test
        @DisplayName("3+ crisis → patrón CRISIS_CYCLE detectado")
        void crisisCycle() {
            List<CriticalDay> crises = List.of(
                    crisis(LocalDateTime.now().minusDays(10)),
                    crisis(LocalDateTime.now().minusDays(20)),
                    crisis(LocalDateTime.now().minusDays(30))
            );
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(crises);

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.detectedPatterns())
                    .anyMatch(p -> "CRISIS_CYCLE".equals(p.pattern()));
        }

        @Test
        @DisplayName("2 crisis → NO CRISIS_CYCLE (< 3)")
        void noCrisisCycle_twoOnly() {
            List<CriticalDay> crises = List.of(
                    crisis(LocalDateTime.now().minusDays(5)),
                    crisis(LocalDateTime.now().minusDays(15))
            );
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(crises);

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.detectedPatterns())
                    .noneMatch(p -> "CRISIS_CYCLE".equals(p.pattern()));
        }

        @Test
        @DisplayName("consecutiveImprovements=3 en LTS → patrón SUSTAINED_IMPROVEMENT")
        void sustainedImprovement() {
            FamilyLongitudinalState ltsVal = lts(0, 3, 0, false, 50.0, null);
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.detectedPatterns())
                    .anyMatch(p -> "SUSTAINED_IMPROVEMENT".equals(p.pattern()));
        }

        @Test
        @DisplayName("gratitud después de crisis → GRATITUDE_AS_RECOVERY detectado")
        void gratitudeAsRecovery() {
            LocalDateTime crisisAt = LocalDateTime.now().minusDays(5);
            LocalDateTime gratAt   = LocalDateTime.now().minusDays(3); // dentro de 7 días post-crisis

            CriticalDay crisis = crisis(crisisAt);
            FamilyGratitudeEntry grat = FamilyGratitudeEntry.builder()
                    .createdAt(gratAt).build();

            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(crisis));
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(grat));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.detectedPatterns())
                    .anyMatch(p -> "GRATITUDE_AS_RECOVERY".equals(p.pattern()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute() — riqueza de datos
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("riqueza de datos (dataRichness)")
    class DataRichness {

        @Test
        @DisplayName("0 eventos → LOW")
        void noEvents_low() {
            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.dataRichness()).isEqualTo("LOW");
        }

        @Test
        @DisplayName("8 eventos en total (grat=5 + evid=3) → MEDIUM")
        void eightEvents_medium() {
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.nCopies(5, recentGratitude()));
            when(evidenceRepository.findByFamilyId(1L))
                    .thenReturn(Collections.nCopies(3, recentEvidence()));

            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.dataRichness()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("20 gratitudes → HIGH")
        void twentyGratitudes_high() {
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.nCopies(20, recentGratitude()));

            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.dataRichness()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("50+ eventos → EXPERT")
        void fiftyEvents_expert() {
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.nCopies(50, recentGratitude()));

            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.dataRichness()).isEqualTo("EXPERT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute() — índice de resiliencia
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("índice de resiliencia")
    class ResilienceIndex {

        @Test
        @DisplayName("sin LTS → resilienceIndex=50.0 (default)")
        void noLts_defaultResilience() {
            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.resilienceIndex()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("LTS con 2 mejoras consecutivas y 1 crisis → base=60, no collapse → 60.0")
        void resilience_withImprovementsAndCrisis() {
            FamilyLongitudinalState ltsVal = lts(0, 2, 0, false, 50.0, null);
            CriticalDay cr = crisis(LocalDateTime.now().minusDays(10));
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(cr));

            DigitalTwinDto dto = service.compute(1L);

            // base = min(100, 40 + 2*10) = 60; communicationCollapse=false → no penalty
            assertThat(dto.resilienceIndex()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("LTS con collapse activo y crisis → resiliencia penalizada -20")
        void resilience_penaltyForCollapse() {
            FamilyLongitudinalState ltsVal = lts(0, 3, 0, true, 30.0, null);
            CriticalDay cr = crisis(LocalDateTime.now().minusDays(5));
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(cr));

            DigitalTwinDto dto = service.compute(1L);

            // base = min(100, 40 + 3*10) = 70; penalty = -20 → 50.0
            assertThat(dto.resilienceIndex()).isEqualTo(50.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // compute() — avgDaysBetweenCrises y avgRecoveryDays
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("métricas de crisis")
    class CrisisMetrics {

        @Test
        @DisplayName("menos de 2 crisis → avgDaysBetweenCrises=null")
        void oneCrisis_nullAvg() {
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(crisis(LocalDateTime.now().minusDays(5))));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.avgDaysBetweenCrises()).isNull();
        }

        @Test
        @DisplayName("2 crisis con 20 días de diferencia → avgDaysBetweenCrises=20")
        void twoCrises_20days() {
            LocalDateTime c1 = LocalDateTime.now().minusDays(25);
            LocalDateTime c2 = LocalDateTime.now().minusDays(5);
            when(crisisRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(crisis(c2), crisis(c1))); // orden DESC como lo devolvería DB

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.avgDaysBetweenCrises()).isEqualTo(20);
        }

        @Test
        @DisplayName("sin LTS → avgRecoveryDays=null")
        void noLts_nullRecovery() {
            DigitalTwinDto dto = service.compute(1L);
            assertThat(dto.avgRecoveryDays()).isNull();
        }

        @Test
        @DisplayName("LTS con crisis30d=0 → avgRecoveryDays=null")
        void ltsNoCrisis_nullRecovery() {
            FamilyLongitudinalState ltsVal = lts(0, 0, 0, false, 50.0, null);
            when(ltsRepository.findByFamilyId(1L)).thenReturn(Optional.of(ltsVal));

            DigitalTwinDto dto = service.compute(1L);

            assertThat(dto.avgRecoveryDays()).isNull();
        }
    }
}
