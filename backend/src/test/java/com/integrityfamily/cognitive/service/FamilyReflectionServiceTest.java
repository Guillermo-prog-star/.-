package com.integrityfamily.cognitive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.FamilyMemory.MemoryType;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyReflectionService — Efectividad, Abandono y Lección Aprendida")
class FamilyReflectionServiceTest {

    @Mock FamilyMemoryRepository           memoryRepository;
    @Mock FamilyIdentityProfileRepository  identityRepository;
    @Mock LearnedSkillRepository           skillRepository;
    @Mock ReflectionRepository             reflectionRepository;
    @Mock LearningEntryRepository          learningEntryRepository;
    @Mock EvaluationRepository             evaluationRepository;
    @Mock FamilyMetricsSnapshotRepository  snapshotRepository;
    @Mock FamilyRepository                 familyRepository;
    @Spy  ObjectMapper                     objectMapper = new ObjectMapper();

    @InjectMocks FamilyReflectionService service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Test Family").build();

        FamilyIdentityProfile defaultProfile = FamilyIdentityProfile.builder().family(family).build();

        lenient().when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
        lenient().when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());
        lenient().when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        lenient().when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(List.of());
        // When no profile exists, orElseGet creates one and saves it — return a valid default
        lenient().when(identityRepository.findByFamilyId(1L)).thenReturn(Optional.empty());
        lenient().when(identityRepository.save(any())).thenReturn(defaultProfile);
        lenient().when(memoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(familyRepository.getReferenceById(1L)).thenReturn(family);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Efectividad de intervención
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Sin evaluaciones ni snapshots → INSUFFICIENT_DATA")
    void noData_returnsInsufficientData() {
        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.effectiveness().level())
                .isEqualTo(FamilyReflectionService.EffectivenessLevel.INSUFFICIENT_DATA);
        assertThat(report.effectiveness().evaluationCount()).isZero();
    }

    @Test
    @DisplayName("ICF +12 puntos, adherencia 75%, reflectionRate 80% → HIGH")
    void strongImprovement_returnsHighEffectiveness() {
        // Dos evaluaciones con ICF subiendo
        List<Evaluation> evals = List.of(
                buildEval(60.0, LocalDateTime.now().minusDays(30)),
                buildEval(72.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);

        // Snapshots con adherencia alta
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(75.0).convivenceIndex(60.0).build(),
                FamilyMetricsSnapshot.builder().adherence(78.0).convivenceIndex(72.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        // Reflecciones completadas al 80%
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(
                buildReflection(ReflectionStatus.COMPLETED),
                buildReflection(ReflectionStatus.COMPLETED),
                buildReflection(ReflectionStatus.COMPLETED),
                buildReflection(ReflectionStatus.COMPLETED),
                buildReflection(ReflectionStatus.DRAFT)
        ));

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.effectiveness().level())
                .isEqualTo(FamilyReflectionService.EffectivenessLevel.HIGH);
        assertThat(report.effectiveness().icfTrend()).isEqualTo(12.0);
        assertThat(report.effectiveness().avgAdherence()).isBetween(75.0, 80.0);
    }

    @Test
    @DisplayName("ICF -20 puntos, adherencia 25% → REGRESSING")
    void sharpDecline_returnsRegressing() {
        List<Evaluation> evals = List.of(
                buildEval(80.0, LocalDateTime.now().minusDays(30)),
                buildEval(60.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);

        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(25.0).convivenceIndex(80.0).build(),
                FamilyMetricsSnapshot.builder().adherence(22.0).convivenceIndex(60.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.effectiveness().level())
                .isEqualTo(FamilyReflectionService.EffectivenessLevel.REGRESSING);
        assertThat(report.effectiveness().icfTrend()).isEqualTo(-20.0);
    }

    @Test
    @DisplayName("ICF estable, adherencia moderada, reflections escasas → LOW")
    void stableButLowEngagement_returnsLow() {
        List<Evaluation> evals = List.of(
                buildEval(55.0, LocalDateTime.now().minusDays(10)),
                buildEval(57.0, LocalDateTime.now())  // +2 pts — no es mejora fuerte
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);

        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(45.0).convivenceIndex(55.0).build(),
                FamilyMetricsSnapshot.builder().adherence(48.0).convivenceIndex(57.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        // Sin reflecciones → reflectionRate = 0
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        // score: icfTrend +2 → +1; adherence 46% → +1; reflectionRate 0% → 0; total=2 → MODERATE
        assertThat(report.effectiveness().level())
                .isIn(FamilyReflectionService.EffectivenessLevel.LOW,
                      FamilyReflectionService.EffectivenessLevel.MODERATE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Detección de señales de abandono
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Sin actividad en 14 días → señal INACTIVITY_14D y riesgo HIGH")
    void noRecentActivity_addsInactivitySignal() {
        // Evaluaciones suficientes para pasar de INSUFFICIENT_DATA
        List<Evaluation> evals = List.of(
                buildEval(60.0, LocalDateTime.now().minusDays(30)),
                buildEval(55.0, LocalDateTime.now().minusDays(15))
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(50.0).convivenceIndex(60.0).build(),
                FamilyMetricsSnapshot.builder().adherence(48.0).convivenceIndex(55.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        // Reflection and learning entries older than 30 days → filtered out → "no recent activity"
        Reflection oldReflection = buildReflection(ReflectionStatus.COMPLETED);
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(oldReflection));
        // No recent learnings
        when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.abandonmentRisk().signals()).contains("INACTIVITY_14D");
        assertThat(report.abandonmentRisk().level())
                .isIn(FamilyReflectionService.AbandonmentLevel.HIGH,
                      FamilyReflectionService.AbandonmentLevel.CRITICAL);
    }

    @Test
    @DisplayName("ICF cae más de 15 pts entre últimas 2 evaluaciones → señal ICF_DECLINING")
    void icfDrop_over15pts_addsIcfDecliningSignal() {
        List<Evaluation> evals = List.of(
                buildEval(70.0, LocalDateTime.now().minusDays(20)),
                buildEval(50.0, LocalDateTime.now().minusDays(5))  // -20 pts
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(55.0).convivenceIndex(70.0).build(),
                FamilyMetricsSnapshot.builder().adherence(52.0).convivenceIndex(50.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        // Recent reflection to avoid INACTIVITY signal
        Reflection recent = Reflection.builder()
                .family(family).status(ReflectionStatus.COMPLETED)
                .communicationImproved(false).emotionalImpact(3)
                .repeatIntent(true).createdAt(LocalDateTime.now().minusDays(2)).build();
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(recent));

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.abandonmentRisk().signals()).contains("ICF_DECLINING");
    }

    @Test
    @DisplayName("2+ reflexiones con repeatIntent=false → señal NEGATIVE_REPEAT_INTENT")
    void negativeRepeatIntent_addsSignal() {
        List<Evaluation> evals = List.of(
                buildEval(60.0, LocalDateTime.now().minusDays(15)),
                buildEval(58.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(50.0).convivenceIndex(60.0).build(),
                FamilyMetricsSnapshot.builder().adherence(48.0).convivenceIndex(58.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        List<Reflection> reflections = List.of(
                buildRecentReflection(false),  // repeatIntent=false
                buildRecentReflection(false),  // repeatIntent=false
                buildRecentReflection(true)    // repeatIntent=true
        );
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(reflections);

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.abandonmentRisk().signals()).contains("NEGATIVE_REPEAT_INTENT");
    }

    @Test
    @DisplayName("Sin señales → riesgo LOW")
    void noSignals_returnsLowRisk() {
        List<Evaluation> evals = List.of(
                buildEval(65.0, LocalDateTime.now().minusDays(10)),
                buildEval(68.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(70.0).convivenceIndex(65.0).build(),
                FamilyMetricsSnapshot.builder().adherence(72.0).convivenceIndex(68.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);

        // Active recent reflections with positive intent
        List<Reflection> reflections = List.of(
                buildRecentReflection(true),
                buildRecentReflection(true)
        );
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(reflections);

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.abandonmentRisk().signals()).doesNotContain("INACTIVITY_14D");
        assertThat(report.abandonmentRisk().signals()).doesNotContain("ICF_DECLINING");
        assertThat(report.abandonmentRisk().level())
                .isEqualTo(FamilyReflectionService.AbandonmentLevel.LOW);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. requiresUrgentAttention
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("REGRESSING + CRITICAL → requiresUrgentAttention = true")
    void regressingAndCriticalAbandonment_requiresUrgentAttention() {
        // Provoke REGRESSING: ICF -20
        List<Evaluation> evals = List.of(
                buildEval(80.0, LocalDateTime.now().minusDays(30)),
                buildEval(60.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(20.0).convivenceIndex(80.0).build(),
                FamilyMetricsSnapshot.builder().adherence(18.0).convivenceIndex(60.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);
        // No recent activity (INACTIVITY) + ICF_DECLINING → riskScore ≥ 5 → CRITICAL
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
        when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.requiresUrgentAttention()).isTrue();
    }

    @Test
    @DisplayName("LOW + LOW → requiresUrgentAttention = false")
    void healthyFamily_doesNotRequireUrgentAttention() {
        List<Evaluation> evals = List.of(
                buildEval(65.0, LocalDateTime.now().minusDays(10)),
                buildEval(78.0, LocalDateTime.now())
        );
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(evals);
        List<FamilyMetricsSnapshot> snaps = List.of(
                FamilyMetricsSnapshot.builder().adherence(75.0).convivenceIndex(65.0).build(),
                FamilyMetricsSnapshot.builder().adherence(80.0).convivenceIndex(78.0).build()
        );
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(snaps);
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(
                buildRecentReflection(true), buildRecentReflection(true),
                buildRecentReflection(true), buildRecentReflection(true)
        ));

        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.requiresUrgentAttention()).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. Report structure
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ReflectionReport siempre tiene familyId, generatedAt y ambas sub-secciones")
    void report_alwaysHasRequiredFields() {
        FamilyReflectionService.ReflectionReport report = service.reflect(1L);

        assertThat(report.familyId()).isEqualTo(1L);
        assertThat(report.generatedAt()).isNotNull();
        assertThat(report.effectiveness()).isNotNull();
        assertThat(report.abandonmentRisk()).isNotNull();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Evaluation buildEval(double icf, LocalDateTime finalizedAt) {
        return Evaluation.builder()
                .id((long) (Math.random() * 1000))
                .family(family).icf(icf)
                .riskLevel("MODERADO").hasCrisis(false)
                .status(EvaluationStatus.FINALIZED)
                .finalizedAt(finalizedAt)
                .build();
    }

    private Reflection buildReflection(ReflectionStatus status) {
        return Reflection.builder()
                .id((long) (Math.random() * 1000))
                .family(family).status(status)
                .communicationImproved(true).emotionalImpact(4)
                .repeatIntent(true)
                .createdAt(LocalDateTime.now().minusDays(40))  // older than 30d → won't be "recent"
                .build();
    }

    private Reflection buildRecentReflection(boolean repeatIntent) {
        return Reflection.builder()
                .id((long) (Math.random() * 1000))
                .family(family).status(ReflectionStatus.COMPLETED)
                .communicationImproved(true).emotionalImpact(4)
                .repeatIntent(repeatIntent)
                .createdAt(LocalDateTime.now().minusDays(5))  // within 30d → "recent"
                .build();
    }
}
