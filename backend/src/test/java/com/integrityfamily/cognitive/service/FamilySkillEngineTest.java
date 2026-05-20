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
@DisplayName("FamilySkillEngine — Matching, Extracción y Métricas de Skills")
class FamilySkillEngineTest {

    @Mock LearnedSkillRepository           skillRepository;
    @Mock FamilyMemoryRepository           memoryRepository;
    @Mock FamilyIdentityProfileRepository  identityRepository;
    @Mock ReflectionRepository             reflectionRepository;
    @Mock LearningEntryRepository          learningEntryRepository;
    @Mock FamilyMetricsSnapshotRepository  snapshotRepository;
    @Mock FamilyRepository                 familyRepository;
    @Spy  ObjectMapper                     objectMapper = new ObjectMapper();

    @InjectMocks FamilySkillEngine engine;

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Test Family").build();
        evaluation = Evaluation.builder()
                .id(10L).family(family).icf(65.0)
                .riskLevel("MODERADO").hasCrisis(false)
                .status(EvaluationStatus.FINALIZED)
                .dimensionScores(List.of())
                .build();

        lenient().when(familyRepository.getReferenceById(1L)).thenReturn(family);
        lenient().when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        lenient().when(identityRepository.findByFamilyId(1L)).thenReturn(Optional.empty());
        lenient().when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
        lenient().when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());
        lenient().when(memoryRepository.findProceduralMemories(1L)).thenReturn(List.of());
        lenient().when(memoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ─── Sin skills disponibles ───────────────────────────────────────────────

    @Test
    @DisplayName("Sin skills de alta confianza → resultado vacío, sin memoria procedural guardada")
    void noHighConfidenceSkills_returnsEmptyResult() {
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of());

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.appliedSkills()).isEmpty();
        assertThat(result.hasNewSkill()).isFalse();
        verify(memoryRepository, never()).save(any());
    }

    // ─── Matching de condiciones ──────────────────────────────────────────────

    @Test
    @DisplayName("Skill con condición 'low_adherence' aplica cuando adherencia < 40")
    void skill_lowAdherence_matchesWhenAdherenceBelowThreshold() throws Exception {
        // Snapshot con adherencia = 30 (< 40 → low_adherence match)
        FamilyMetricsSnapshot snap = FamilyMetricsSnapshot.builder()
                .adherence(30.0).convivenceIndex(55.0).build();
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L))
                .thenReturn(List.of(snap));

        LearnedSkill skill = buildSkill("skill-low-adherence",
                "[\"low_adherence\"]",
                "[\"reduce-task-load\"]");
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of(skill));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.appliedSkills()).hasSize(1);
        assertThat(result.appliedSkills().get(0).getSkillName()).isEqualTo("skill-low-adherence");

        // Debe guardar una memoria procedural
        ArgumentCaptor<FamilyMemory> memCaptor = ArgumentCaptor.forClass(FamilyMemory.class);
        verify(memoryRepository, atLeastOnce()).save(memCaptor.capture());
        FamilyMemory saved = memCaptor.getAllValues().stream()
                .filter(m -> m.getMemoryType() == MemoryType.PROCEDURAL)
                .findFirst().orElseThrow();
        assertThat(saved.getSemanticKey()).isEqualTo("active-skill:skill-low-adherence");
        assertThat(saved.getSourceType()).isEqualTo("SKILL_ENGINE");
        assertThat(saved.getExpiresAt()).isAfterOrEqualTo(LocalDateTime.now().plusDays(29));
    }

    @Test
    @DisplayName("Skill con condición 'high_stress_triggers' aplica cuando hasCrisis=true")
    void skill_highStressTriggers_matchesWhenCrisisActive() throws Exception {
        Evaluation crisisEval = Evaluation.builder()
                .id(11L).family(family).icf(35.0)
                .riskLevel("CRÍTICO").hasCrisis(true)
                .status(EvaluationStatus.FINALIZED)
                .dimensionScores(List.of())
                .build();

        LearnedSkill skill = buildSkill("skill-crisis",
                "[\"high_stress_triggers\"]",
                "[\"de-escalation-protocol\"]");
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of(skill));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, crisisEval);

        assertThat(result.appliedSkills()).hasSize(1);
    }

    @Test
    @DisplayName("Skill con condición 'adherence_gt_60' NO aplica cuando adherencia < 60")
    void skill_adherenceGt60_doesNotMatchWhenLow() throws Exception {
        // Snapshot con adherencia = 45 (no cumple adherence_gt_60)
        FamilyMetricsSnapshot snap = FamilyMetricsSnapshot.builder()
                .adherence(45.0).convivenceIndex(60.0).build();
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L))
                .thenReturn(List.of(snap));

        LearnedSkill skill = buildSkill("skill-high-adherence",
                "[\"adherence_gt_60\"]",
                "[\"reinforce-positive\"]");
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of(skill));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.appliedSkills()).isEmpty();
    }

    @Test
    @DisplayName("Skill con múltiples condiciones: aplica si cumple al menos la mitad")
    void skill_halfConditionsMet_isApplied() throws Exception {
        // Conditions: ["low_adherence", "adherence_gt_60"]
        // low_adherence = false (adherence=65), adherence_gt_60 = true (65 > 60)
        // → 1 de 2 = 50% = ceil(2/2.0) = 1 → APLICA
        FamilyMetricsSnapshot snap = FamilyMetricsSnapshot.builder()
                .adherence(65.0).convivenceIndex(60.0).build();
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L))
                .thenReturn(List.of(snap));

        LearnedSkill skill = buildSkill("skill-mixed",
                "[\"low_adherence\",\"adherence_gt_60\"]",
                "[\"strategy\"]");
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of(skill));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.appliedSkills()).hasSize(1);
    }

    // ─── Extracción de nueva skill ────────────────────────────────────────────

    @Test
    @DisplayName("Menos de 3 reflexiones → no se extrae nueva skill")
    void fewerThanThreeReflections_noNewSkill() throws Exception {
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of());
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(
                buildReflection(true, 5, true),
                buildReflection(true, 4, true)
        ));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.hasNewSkill()).isFalse();
    }

    @Test
    @DisplayName("3+ reflexiones exitosas con tasa comunicación > 60% → nueva skill extraída")
    void threeSuccessfulReflections_withHighCommunicationRate_extractsNewSkill() throws Exception {
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of());

        List<Reflection> reflections = List.of(
                buildReflection(true, 5, true),  // communicationImproved=true
                buildReflection(true, 4, true),  // communicationImproved=true
                buildReflection(true, 5, true),  // communicationImproved=true
                buildReflection(false, 5, true)  // no communicationImproved
        );
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(reflections);
        when(skillRepository.existsBySkillName(any())).thenReturn(false);
        when(skillRepository.save(any(LearnedSkill.class))).thenAnswer(i -> i.getArgument(0));

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.hasNewSkill()).isTrue();
        assertThat(result.newSkillExtracted().getConfidence()).isGreaterThan(0.5);
        verify(skillRepository, times(1)).save(any(LearnedSkill.class));
    }

    @Test
    @DisplayName("Skill con mismo nombre ya existe → no se extrae duplicado")
    void duplicateSkillName_noDuplicate() throws Exception {
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of());

        List<Reflection> reflections = List.of(
                buildReflection(true, 5, true),
                buildReflection(true, 4, true),
                buildReflection(true, 5, true)
        );
        when(reflectionRepository.findByFamilyId(1L)).thenReturn(reflections);
        when(skillRepository.existsBySkillName(any())).thenReturn(true); // ya existe

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.hasNewSkill()).isFalse();
        verify(skillRepository, never()).save(any());
    }

    // ─── Resultado structural ─────────────────────────────────────────────────

    @Test
    @DisplayName("SkillEngineResult expone correctamente las conditions del análisis")
    void result_exposesConditionsOfAnalysis() {
        when(skillRepository.findHighConfidenceSkills(anyDouble())).thenReturn(List.of());

        FamilySkillEngine.SkillEngineResult result = engine.analyze(1L, evaluation);

        assertThat(result.conditions()).isNotNull();
        assertThat(result.conditions().familyId()).isEqualTo(1L);
        assertThat(result.conditions().icf()).isEqualTo(65.0);
        assertThat(result.conditions().hasCrisis()).isFalse();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private LearnedSkill buildSkill(String name, String conditions, String strategy) {
        return LearnedSkill.builder()
                .id(1L)
                .skillName(name)
                .description("Test skill")
                .conditions(conditions)
                .recommendedStrategy(strategy)
                .confidence(0.8)
                .successRate(0.7)
                .createdByAi(true)
                .build();
    }

    private Reflection buildReflection(boolean commImproved, int impact, boolean repeatIntent) {
        return Reflection.builder()
                .id((long) (Math.random() * 1000))
                .family(family)
                .status(ReflectionStatus.COMPLETED)
                .communicationImproved(commImproved)
                .emotionalImpact(impact)
                .repeatIntent(repeatIntent)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
    }
}
