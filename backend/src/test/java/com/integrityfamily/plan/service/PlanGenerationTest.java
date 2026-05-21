package com.integrityfamily.plan.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.plan.dto.PlanDtos.*;
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
public class PlanGenerationTest {

    @Mock
    private ImprovementPlanRepository planRepository;

    @Mock
    private PlanTaskRepository planTaskRepository;

    @Mock
    private FamilyLogbookEntryRepository logbookEntryRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private PlanTemplateRepository planTemplateRepository;

    @Mock
    private PlanTemplateActivityRepository planTemplateActivityRepository;

    @InjectMocks
    private PlanService planService;

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .criticalDimension("comunicacion")
                .riskLevel("HIGH")
                .status(EvaluationStatus.FINALIZED)
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Caso 1: Generar Plan Determinístico con 4 fases clínicas exitosamente")
    void shouldGenerateDeterministicPlanSuccessfully() {
        Mockito.when(evaluationRepository.findById(100L)).thenReturn(Optional.of(evaluation));
        
        // Simular que no hay plantilla guardada inicialmente para probar la autogeneración de respaldo
        Mockito.when(planTemplateRepository.findByDimensionAndRiskLevel("comunicacion", "HIGH")).thenReturn(List.of());
        Mockito.when(planTemplateRepository.findByDimension("comunicacion")).thenReturn(List.of());
        
        PlanTemplate template = PlanTemplate.builder()
                .code("TMPL-COMUNICACION-HIGH")
                .name("Plan de Transformación en comunicacion")
                .dimension("comunicacion")
                .riskLevel("HIGH")
                .build();
        Mockito.when(planTemplateRepository.save(any(PlanTemplate.class))).thenReturn(template);

        // Simular autoguardado del plan
        ImprovementPlan savedPlan = ImprovementPlan.builder()
                .id(500L)
                .family(family)
                .evaluation(evaluation)
                .title("Plan de Transformación: " + template.getName())
                .description("Intervención clínica ensamblada determinísticamente para el nivel de riesgo HIGH en la dimensión comunicacion.")
                .tasks(new ArrayList<>())
                .build();
        Mockito.when(planRepository.save(any(ImprovementPlan.class))).thenReturn(savedPlan);

        // Simular que no hay actividades para sembrarlas
        Mockito.when(planTemplateActivityRepository.findByTemplateCode("TMPL-COMUNICACION-HIGH")).thenReturn(List.of());
        Mockito.when(planTemplateActivityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.when(planTaskRepository.save(any(PlanTask.class))).thenAnswer(invocation -> {
            PlanTask task = invocation.getArgument(0);
            task.setId(System.nanoTime());
            return task;
        });

        PlanResponse response = planService.generateDeterministicPlan(100L);

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals(1L, response.familyId());
        assertEquals(100L, response.evaluationId());
        assertEquals("Plan de Transformación: Plan de Transformación en comunicacion", response.title());
        assertEquals(4, response.tasks().size());

        // Verificar fases
        assertEquals("RECONOCIMIENTO", response.tasks().get(0).fase());
        assertEquals("RECONOCIMIENTO", response.tasks().get(1).fase());
        assertEquals("RECONOCIMIENTO", response.tasks().get(2).fase());
        assertEquals("AMOR", response.tasks().get(3).fase());
    }

    @Test
    @DisplayName("Caso 2: Lanzar excepción si la evaluación no existe")
    void shouldThrowExceptionWhenEvaluationNotFound() {
        Mockito.when(evaluationRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> planService.generateDeterministicPlan(999L));
        assertEquals("Evaluación no encontrada: 999", exception.getMessage());
    }
}
