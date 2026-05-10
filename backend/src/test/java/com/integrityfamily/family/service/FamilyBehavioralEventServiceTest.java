package com.integrityfamily.family.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyBehavioralEvent;
import com.integrityfamily.domain.repository.FamilyBehavioralEventRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.dto.FamilyIvrSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyBehavioralEventServiceTest {

    @Mock
    private FamilyBehavioralEventRepository eventRepository;

    @Mock
    private FamilyRepository familyRepository;

    @InjectMocks
    private FamilyBehavioralEventService behavioralEventService;

    private Family testFamily;

    @BeforeEach
    void setUp() {
        testFamily = Family.builder()
                .id(1L)
                .name("Familia López")
                .currentMilestone("M12")
                .sentinelActive(false)
                .build();
    }

    @Test
    void testCalculateIvr_PerfectPeace() {
        // GIVEN: Sin fricciones conductuales registradas
        when(eventRepository.findByFamilyIdOrderByOccurredAtDesc(1L)).thenReturn(Collections.emptyList());

        // WHEN: Se calcula el IVR
        FamilyIvrSummary summary = behavioralEventService.calculateIvr(1L);

        // THEN: El score debe ser 100.0 (Perfect Peace) y sin conflictos
        assertNotNull(summary);
        assertEquals(100.0, summary.ivrScore());
        assertEquals(0, summary.totalConflicts());
        assertEquals(0, summary.repairedConflicts());
        assertEquals(0.0, summary.averageRepairTimeHours());
    }

    @Test
    void testCalculateIvr_WithFrictions() {
        // GIVEN: Dos fricciones, una reparada rápidamente y otra pendiente por 10 horas
        LocalDateTime now = LocalDateTime.now();
        
        // Conflicto 1: Reparado en 2 horas (muy rápido)
        FamilyBehavioralEvent event1 = FamilyBehavioralEvent.builder()
                .id(101L)
                .family(testFamily)
                .severity(2)
                .occurredAt(now.minusHours(5))
                .repairedAt(now.minusHours(3))
                .repairDescription("Diálogo constructivo")
                .build();

        // Conflicto 2: Pendiente sin reparar desde hace 10 horas (penalización exponencial activa)
        FamilyBehavioralEvent event2 = FamilyBehavioralEvent.builder()
                .id(102L)
                .family(testFamily)
                .severity(3)
                .occurredAt(now.minusHours(10))
                .build();

        when(eventRepository.findByFamilyIdOrderByOccurredAtDesc(1L)).thenReturn(Arrays.asList(event2, event1));

        // WHEN: Calculamos el IVR
        FamilyIvrSummary summary = behavioralEventService.calculateIvr(1L);

        // THEN: El IVR debe reflejar la penalización exponencial y la latencia promedio correcta
        assertNotNull(summary);
        assertEquals(2, summary.totalConflicts());
        assertEquals(1, summary.repairedConflicts());
        assertEquals(2.0, summary.averageRepairTimeHours()); // (3h - 5h) = 2 horas latencia
        assertTrue(summary.ivrScore() < 100.0, "El score IVR debe haber sido castigado por el conflicto pendiente de 10 horas");
    }
}
