package com.integrityfamily.risk.service;

import com.integrityfamily.admin.service.SecurityWatchdogService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private RiskSnapshotRepository riskSnapshotRepository;

    @Mock
    private SecurityWatchdogService watchdogService;

    @InjectMocks
    private RiskService riskService;

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
    void testCalculateMonthsSinceRegistration_Normal() {
        // GIVEN: Un hito numérico estándar
        testFamily.setCurrentMilestone("M12");
        when(riskSnapshotRepository.save(any(RiskSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: Ejecutamos el motor de riesgos
        RiskSnapshot snapshot = riskService.calculateAndCreate(testFamily, 85.0, false);

        // THEN: Debe parsear 12 meses, resultando en bajo riesgo
        assertNotNull(snapshot);
        assertEquals("BAJO", snapshot.getRiskLevel());
    }

    @Test
    void testCalculateMonthsSinceRegistration_LongString_NoCrash() {
        // GIVEN: Un hito largo que causaría NumberFormatException en el código viejo
        testFamily.setCurrentMilestone("Hito 1 - Reconstrucción y Reconciliación con Sofía de Armenia");
        when(riskSnapshotRepository.save(any(RiskSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: Ejecutamos el motor sin crashear
        RiskSnapshot snapshot = riskService.calculateAndCreate(testFamily, 85.0, false);

        // THEN: Debe parsear el número de mes 1 de forma segura y el cálculo debe completarse con éxito
        assertNotNull(snapshot);
        assertEquals("BAJO", snapshot.getRiskLevel());
    }

    @Test
    void testCalculateMonthsSinceRegistration_BlankOrNull_NoCrash() {
        // GIVEN: Hito nulo o en blanco
        testFamily.setCurrentMilestone(null);
        when(riskSnapshotRepository.save(any(RiskSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN & THEN: Se ejecuta sin lanzar excepciones
        RiskSnapshot snapshot = riskService.calculateAndCreate(testFamily, 30.0, false);
        assertNotNull(snapshot);
        assertEquals("ALTO", snapshot.getRiskLevel()); // icf 30 con 0 meses es ALTO (umbral medio es 40)
    }

    @Test
    void testCalculateDynamicRisk_WithCrisisActive() {
        // GIVEN: Estado de crisis activo forzado
        testFamily.setCurrentMilestone("M3");
        when(riskSnapshotRepository.save(any(RiskSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: Se calcula con hasCrisis = true
        RiskSnapshot snapshot = riskService.calculateAndCreate(testFamily, 95.0, true);

        // THEN: El nivel de riesgo debe ser CRÍTICO y el centinela activado
        assertEquals("CRITICO", snapshot.getRiskLevel());
        assertTrue(testFamily.getSentinelActive());
        verify(watchdogService, times(1)).scanForAnomalies();
    }
}
