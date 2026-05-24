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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyMemoryService — Captura e Importancia de Memorias")
class FamilyMemoryServiceTest {

    @Mock FamilyMemoryRepository         memoryRepository;
    @Mock FamilyIdentityProfileRepository identityRepository;
    @Mock FamilyRepository               familyRepository;
    @Mock EvaluationRepository           evaluationRepository;
    @Mock ReflectionRepository           reflectionRepository;
    @Mock LearningEntryRepository        learningEntryRepository;

    @Spy  ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks FamilyMemoryService memoryService;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Test").build();
        Mockito.lenient().when(memoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Mockito.lenient().when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of());
    }

    // ─── Importancia de episodios de evaluación ──────────────────────────────

    @Test
    @DisplayName("ICF < 40 → importancia 0.95 (crisis)")
    void crisisIcf_highestImportance() {
        Evaluation eval = buildEval(1L, 35.0, false);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getImportanceScore()).isEqualTo(0.95);
        assertThat(memory.getMemoryType()).isEqualTo(MemoryType.EPISODIC);
        assertThat(memory.getSemanticKey()).isEqualTo("evaluation-result");
    }

    @Test
    @DisplayName("hasCrisis = true → importancia 1.0 (máximo absoluto)")
    void activeCrisis_maximumImportance() {
        Evaluation eval = buildEval(2L, 55.0, true);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getImportanceScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("ICF entre 60 y 79 → importancia 0.5 (normal)")
    void normalIcf_defaultImportance() {
        Evaluation eval = buildEval(3L, 65.0, false);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getImportanceScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("ICF ≥ 80 → importancia 0.70 (buena noticia, merece registro)")
    void highIcf_moderateImportance() {
        Evaluation eval = buildEval(4L, 85.0, false);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getImportanceScore()).isEqualTo(0.70);
    }

    @Test
    @DisplayName("ICF entre 40 y 59 → importancia 0.80")
    void mediumRiskIcf_elevatedImportance() {
        Evaluation eval = buildEval(5L, 50.0, false);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getImportanceScore()).isEqualTo(0.80);
    }

    // ─── Contenido de la memoria ─────────────────────────────────────────────

    @Test
    @DisplayName("captureEvaluationMemory guarda evaluationId, icf y riskLevel en el JSON")
    void capturedMemory_containsEssentialFields() throws Exception {
        Evaluation eval = buildEval(42L, 62.0, false);

        FamilyMemory memory = memoryService.captureEvaluationMemory(eval);

        assertThat(memory.getSourceType()).isEqualTo("EVALUATION");
        assertThat(memory.getSourceId()).isEqualTo(42L);
        assertThat(memory.getContent()).contains("evaluationId");
        assertThat(memory.getContent()).contains("icf");
    }

    // ─── Consolidación semántica ─────────────────────────────────────────────

    @Test
    @DisplayName("Menos de 3 episodios → consolidación semántica NO se ejecuta")
    void fewEpisodes_noConsolidation() {
        when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(1L, "evaluation-result"))
                .thenReturn(List.of(buildMemory(), buildMemory())); // solo 2

        memoryService.consolidateSemanticPattern(1L, "evaluation-result");

        // No debería haberse guardado ningún patrón semántico
        verify(memoryRepository, never()).save(argThat(m ->
                m.getMemoryType() == MemoryType.SEMANTIC));
    }

    @Test
    @DisplayName("3 o más episodios → consolidación semántica SE ejecuta")
    void threeEpisodes_triggersConsolidation() throws Exception {
        String episode = objectMapper.writeValueAsString(
                java.util.Map.of("icf", 65.0, "criticalDimension", "comunicacion"));

        FamilyMemory ep1 = buildMemoryWithContent(episode);
        FamilyMemory ep2 = buildMemoryWithContent(episode);
        FamilyMemory ep3 = buildMemoryWithContent(episode);

        when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(1L, "evaluation-result"))
                .thenReturn(List.of(ep1, ep2, ep3));
        when(memoryRepository.findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(1L, "evaluation-trend-pattern"))
                .thenReturn(List.of());
        Mockito.lenient().when(familyRepository.getReferenceById(1L)).thenReturn(family);

        memoryService.consolidateSemanticPattern(1L, "evaluation-result");

        // Debe haber guardado el patrón semántico
        verify(memoryRepository, atLeastOnce()).save(argThat(m ->
                m.getMemoryType() == MemoryType.SEMANTIC
                && "evaluation-trend-pattern".equals(m.getSemanticKey())));
    }

    // ─── Reflexión episódica ──────────────────────────────────────────────────

    @Test
    @DisplayName("Reflexión con communicationImproved=true eleva importancia")
    void reflectionWithCommImproved_higherImportance() {
        Reflection r = Reflection.builder()
                .id(1L).family(family)
                .emotionalImpact(3).communicationImproved(true)
                .repeatIntent(true).createdAt(LocalDateTime.now())
                .status(ReflectionStatus.COMPLETED).build();
        when(memoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FamilyMemory memory = memoryService.captureReflectionMemory(r);

        // base 0.4 + communicationImproved 0.2 = 0.6
        assertThat(memory.getImportanceScore()).isGreaterThanOrEqualTo(0.6);
    }

    @Test
    @DisplayName("Reflexión con repeatIntent=false añade señal de abandono")
    void reflectionWithNoRepeatIntent_higherImportance() {
        Reflection r = Reflection.builder()
                .id(2L).family(family)
                .emotionalImpact(2).communicationImproved(false)
                .repeatIntent(false).createdAt(LocalDateTime.now())
                .status(ReflectionStatus.DRAFT).build();
        when(memoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FamilyMemory memory = memoryService.captureReflectionMemory(r);

        // base 0.4 + repeatIntent=false 0.1 = 0.5
        assertThat(memory.getImportanceScore()).isGreaterThanOrEqualTo(0.5);
        assertThat(memory.getSemanticKey()).isEqualTo("reflection-emotional");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Evaluation buildEval(Long id, double icf, boolean hasCrisis) {
        return Evaluation.builder()
                .id(id).family(family).icf(icf)
                .riskLevel("MODERADO").hasCrisis(hasCrisis)
                .status(EvaluationStatus.FINALIZED)
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    private FamilyMemory buildMemory() {
        return FamilyMemory.builder()
                .family(family).memoryType(MemoryType.EPISODIC)
                .semanticKey("evaluation-result")
                .content("{\"icf\":65.0}")
                .importanceScore(0.7).sourceType("EVALUATION").build();
    }

    private FamilyMemory buildMemoryWithContent(String content) {
        return FamilyMemory.builder()
                .family(family).memoryType(MemoryType.EPISODIC)
                .semanticKey("evaluation-result")
                .content(content)
                .importanceScore(0.7).sourceType("EVALUATION").build();
    }
}
