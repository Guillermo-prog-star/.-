package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.dto.ConvivenceAnalyticsDto.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ConvivenceAnalyticsTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private PlanTaskRepository planTaskRepository;

    @Mock
    private TaskEvidenceRepository taskEvidenceRepository;

    @Mock
    private ReflectionRepository reflectionRepository;

    @Mock
    private FamilyMetricsSnapshotRepository snapshotRepository;

    @InjectMocks
    private ConvivenceAnalyticsService analyticsService;

    private Family family;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").members(List.of(new FamilyMember(), new FamilyMember())).build();
        evaluation = Evaluation.builder()
                .id(100L)
                .family(family)
                .icf(80.0) // Puntaje base alto
                .dimensionScores(List.of(
                    EvaluationDimensionScore.builder().dimensionName("emociones").score(85.0).build(),
                    EvaluationDimensionScore.builder().dimensionName("comunicacion").score(20.0).build() // < 25 para disparar alerta
                ))
                .finalizedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Caso 1: Calcular Dashboard Operativo y evaluar alertas exitosamente")
    void shouldCalculateOperativeDashboardSuccessfully() {
        Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        Mockito.when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of(evaluation));
        Mockito.when(planTaskRepository.findAll()).thenReturn(List.of()); // 0 tareas asignadas -> adherencia 100%
        Mockito.when(taskEvidenceRepository.findAll()).thenReturn(List.of());
        Mockito.when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
        Mockito.when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(1L)).thenReturn(List.of());
        Mockito.when(snapshotRepository.save(any(FamilyMetricsSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        OperativeDashboardResponse res = analyticsService.getOperativeDashboard(1L);

        assertNotNull(res);
        assertEquals(1L, res.familyId());
        // convivenceIndex = (80 * 0.4) + (100 * 0.2) + (85 * 0.15) + (50 * 0.15) + (70 * 0.1) = 32 + 20 + 12.75 + 7.5 + 7 = 79.25 (Estable)
        assertEquals(79.25, res.convivenceIndex(), 0.1);
        assertEquals("Estable", res.convivenceStatus());
        assertEquals("Alta", res.adherenceStatus());

        // Verificar alertas: Comunicación = 20 (< 25) y sin evidencias en 14 días
        assertFalse(res.activeAlerts().isEmpty());
        assertTrue(res.activeAlerts().stream().anyMatch(a -> a.alertCode().equals("ALERTA_CRITICAL_COMMUNICATION")));
        assertTrue(res.activeAlerts().stream().anyMatch(a -> a.alertCode().equals("ALERTA_INACTIVITY")));
    }
}
