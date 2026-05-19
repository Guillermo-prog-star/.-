package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.CopilotDtos.*;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class CopilotServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private PlanTaskRepository planTaskRepository;

    @Mock
    private TaskEvidenceRepository taskEvidenceRepository;

    @Mock
    private LearningEntryRepository learningEntryRepository;

    @Mock
    private FamilyMetricsSnapshotRepository snapshotRepository;

    @Mock
    private AiInferenceRepository inferenceRepository;

    @Mock
    private AiProvider aiProvider;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private com.integrityfamily.ai.config.AiProperties aiProperties;

    @InjectMocks
    private CopilotService copilotService;

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .riskLevel("HIGH")
                .criticalDimension("comunicacion")
                .finalizedAt(LocalDateTime.now())
                .build();
        
        Mockito.lenient().when(aiProperties.getAnthropic()).thenReturn(new com.integrityfamily.ai.config.AiProperties.Anthropic());
    }

    @Test
    @DisplayName("Caso 1: Construir resumen estructurado compacto exitosamente (Context Builder)")
    void shouldBuildContextSuccessfully() {
        Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        Mockito.when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(List.of(evaluation));
        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        Mockito.when(planTaskRepository.findAll()).thenReturn(List.of());
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of());
        Mockito.when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());

        CompactFamilyContext context = copilotService.buildContext(1L);

        assertNotNull(context);
        assertEquals(1L, context.familyId());
        assertEquals("HIGH", context.riskLevel());
        assertEquals("comunicacion", context.criticalDimension());
        assertEquals("ESTABLE", context.trend());
    }

    @Test
    @DisplayName("Caso 2: Generar inferencia con fallback determinístico y persistir en Gobernanza")
    void shouldGenerateInferenceAndPersistForGovernance() throws Exception {
        Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        Mockito.when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(List.of(evaluation));
        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        Mockito.when(planTaskRepository.findAll()).thenReturn(List.of());
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of());
        Mockito.when(learningEntryRepository.findByFamilyId(1L)).thenReturn(List.of());

        // aiProvider.generateRawResponse devuelve null → NPE en replace() → fallback determinístico se activa
        Mockito.when(objectMapper.writeValueAsString(any())).thenReturn("{\"familyId\": 1}");

        Mockito.when(inferenceRepository.save(any(AiInferenceEntity.class))).thenAnswer(i -> i.getArgument(0));

        CopilotInferRequest req = new CopilotInferRequest(1L, "NEW_ALERT");
        StructuredAiInferenceResponse res = copilotService.generateInference(req);

        assertNotNull(res);
        assertEquals("La familia mantiene adherencia estable y mejora evolutiva gradual.", res.summary());
        assertEquals("LOW", res.priority());
        assertEquals("Consolidar hábitos actuales antes de aumentar intensidad de intervención.", res.containmentSuggestion());

        // Verificar que se guardó inmutablemente para auditoría
        Mockito.verify(inferenceRepository, Mockito.times(1)).save(any(AiInferenceEntity.class));
    }
}
