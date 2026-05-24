package com.integrityfamily.family.service;

import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.dto.CreateFamilyLogbookEntryRequest;
import com.integrityfamily.dto.FamilyLogbookEntryResponse;
import com.integrityfamily.dto.ResolveFamilyLogbookEntryRequest;
import com.integrityfamily.domain.repository.FamilyLogbookEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SDD: Pruebas unitarias para FamilyLogbookService.
 * Verifica la integración con FamilyMemoryService al crear y resolver entradas.
 */
@ExtendWith(MockitoExtension.class)
class FamilyLogbookServiceTest {

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyLogbookEntryRepository logbookRepository;

    @Mock
    private FamilyMemoryService familyMemoryService;

    @InjectMocks
    private FamilyLogbookService logbookService;

    private Family testFamily;
    private FamilyLogbookEntry testEntry;

    @BeforeEach
    void setUp() {
        testFamily = Family.builder()
                .id(1L)
                .name("Familia López")
                .currentMilestone("M12")
                .sentinelActive(false)
                .build();

        testEntry = FamilyLogbookEntry.builder()
                .id(10L)
                .family(testFamily)
                .situation("Conflicto por uso de pantallas")
                .difficultyDetected("Falta de límites claros")
                .emotionIdentified("Frustración")
                .understanding("Los niños necesitan estructura")
                .correctionAction("Establecer horario de pantallas")
                .familyAgreement("Máximo 1 hora de pantallas al día")
                .createdBy("mama@familia.com")
                .status(LogbookStatus.OPEN)
                .build();
    }

    // ─── create() ────────────────────────────────────────────────────────────

    @Test
    void create_savesEntryAndCapturesOpenMemory() {
        // GIVEN
        CreateFamilyLogbookEntryRequest request = new CreateFamilyLogbookEntryRequest(
                1L,
                "Conflicto por uso de pantallas",
                "Falta de límites claros",
                "Frustración",
                "Los niños necesitan estructura",
                "Establecer horario de pantallas",
                "Máximo 1 hora de pantallas al día",
                "mama@familia.com"
        );

        when(familyRepository.findById(1L)).thenReturn(Optional.of(testFamily));
        when(logbookRepository.save(any(FamilyLogbookEntry.class))).thenReturn(testEntry);

        // WHEN
        FamilyLogbookEntryResponse response = logbookService.create(request);

        // THEN: entry saved
        verify(logbookRepository, times(1)).save(any(FamilyLogbookEntry.class));

        // THEN: open memory captured exactly once
        verify(familyMemoryService, times(1)).captureLogbookOpenMemory(testEntry);

        // THEN: resolution memory NOT captured
        verify(familyMemoryService, never()).captureLogbookResolutionMemory(any());

        assertNotNull(response);
        assertEquals(10L, response.id());
    }

    @Test
    void create_familyNotFound_throwsIllegalArgument() {
        // GIVEN
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        CreateFamilyLogbookEntryRequest request = new CreateFamilyLogbookEntryRequest(
                99L, "s", "d", "e", "u", "c", "a", "x"
        );

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> logbookService.create(request));
        verify(logbookRepository, never()).save(any());
        verify(familyMemoryService, never()).captureLogbookOpenMemory(any());
    }

    @Test
    void create_memoryServiceThrows_stillReturnsResponse() {
        // GIVEN: familyMemoryService falla — el servicio debe absorber el error
        CreateFamilyLogbookEntryRequest request = new CreateFamilyLogbookEntryRequest(
                1L, "s", "d", "e", "u", "c", "a", "x"
        );

        when(familyRepository.findById(1L)).thenReturn(Optional.of(testFamily));
        when(logbookRepository.save(any(FamilyLogbookEntry.class))).thenReturn(testEntry);
        doThrow(new RuntimeException("Falla de memoria cognitiva"))
                .when(familyMemoryService).captureLogbookOpenMemory(any());

        // WHEN — no debe lanzar excepción
        FamilyLogbookEntryResponse response = logbookService.create(request);

        // THEN
        assertNotNull(response);
        verify(logbookRepository, times(1)).save(any());
    }

    // ─── resolve() ───────────────────────────────────────────────────────────

    @Test
    void resolve_savesEntryAndCapturesResolutionMemory() {
        // GIVEN
        ResolveFamilyLogbookEntryRequest request = new ResolveFamilyLogbookEntryRequest(
                "Los niños cumplieron el horario durante 2 semanas",
                "papa@familia.com"
        );

        FamilyLogbookEntry resolvedEntry = FamilyLogbookEntry.builder()
                .id(10L)
                .family(testFamily)
                .situation("Conflicto por uso de pantallas")
                .difficultyDetected("Falta de límites claros")
                .emotionIdentified("Frustración")
                .understanding("Los niños necesitan estructura")
                .correctionAction("Establecer horario de pantallas")
                .familyAgreement("Máximo 1 hora de pantallas al día")
                .progressEvidence("Los niños cumplieron el horario durante 2 semanas")
                .resolvedBy("papa@familia.com")
                .status(LogbookStatus.RESOLVED)
                .build();

        when(logbookRepository.findById(10L)).thenReturn(Optional.of(testEntry));
        when(logbookRepository.save(any(FamilyLogbookEntry.class))).thenReturn(resolvedEntry);

        // WHEN
        FamilyLogbookEntryResponse response = logbookService.resolve(10L, request);

        // THEN: entry saved after resolve()
        verify(logbookRepository, times(1)).save(testEntry);

        // THEN: resolution memory captured exactly once
        verify(familyMemoryService, times(1)).captureLogbookResolutionMemory(resolvedEntry);

        // THEN: open memory NOT captured
        verify(familyMemoryService, never()).captureLogbookOpenMemory(any());

        assertNotNull(response);
    }

    @Test
    void resolve_entryNotFound_throwsIllegalArgument() {
        // GIVEN
        when(logbookRepository.findById(999L)).thenReturn(Optional.empty());

        ResolveFamilyLogbookEntryRequest request = new ResolveFamilyLogbookEntryRequest("evidencia", "user");

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> logbookService.resolve(999L, request));
        verify(familyMemoryService, never()).captureLogbookResolutionMemory(any());
    }

    @Test
    void resolve_memoryServiceThrows_stillReturnsResponse() {
        // GIVEN: familyMemoryService falla — el servicio debe absorber el error
        ResolveFamilyLogbookEntryRequest request = new ResolveFamilyLogbookEntryRequest(
                "Evidencia de resolución", "user@test.com"
        );

        when(logbookRepository.findById(10L)).thenReturn(Optional.of(testEntry));
        when(logbookRepository.save(any(FamilyLogbookEntry.class))).thenReturn(testEntry);
        doThrow(new RuntimeException("Falla de memoria cognitiva"))
                .when(familyMemoryService).captureLogbookResolutionMemory(any());

        // WHEN — no debe lanzar excepción
        FamilyLogbookEntryResponse response = logbookService.resolve(10L, request);

        // THEN
        assertNotNull(response);
        verify(logbookRepository, times(1)).save(any());
    }

    // ─── findByFamily() / findById() ─────────────────────────────────────────

    @Test
    void findByFamily_returnsAllEntriesMappedToResponse() {
        // GIVEN
        when(logbookRepository.findByFamilyIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(testEntry));

        // WHEN
        List<FamilyLogbookEntryResponse> result = logbookService.findByFamily(1L);

        // THEN
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        verify(familyMemoryService, never()).captureLogbookOpenMemory(any());
        verify(familyMemoryService, never()).captureLogbookResolutionMemory(any());
    }

    @Test
    void findById_returnsEntry() {
        // GIVEN
        when(logbookRepository.findById(10L)).thenReturn(Optional.of(testEntry));

        // WHEN
        FamilyLogbookEntryResponse response = logbookService.findById(10L);

        // THEN
        assertNotNull(response);
        assertEquals(10L, response.id());
    }

    @Test
    void findById_notFound_throwsIllegalArgument() {
        when(logbookRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> logbookService.findById(99L));
    }
}
