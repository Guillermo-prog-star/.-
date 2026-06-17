package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.provider.TaskType;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.CriticalDayRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiInferenceService")
class AiInferenceServiceTest {

    @Mock AiProviderSelector    aiProviderSelector;
    @Mock AiProvider            aiProvider;
    @Mock FamilyRepository      familyRepository;
    @Mock CriticalDayRepository criticalDayRepository;
    @Mock ContextSynthesizer    contextSynthesizer;
    @InjectMocks AiInferenceService service;

    private static final long FAM_ID = 1L;

    private Family family() {
        return Family.builder().id(FAM_ID).name("García").build();
    }

    @Test
    @DisplayName("familyId no numérico → excepción capturada, no interactúa con repos de dominio")
    void nonNumericFamilyId_silentFailure() {
        service.handleCrisisSignal("abc");

        verifyNoInteractions(familyRepository, criticalDayRepository, contextSynthesizer, aiProvider);
    }

    @Test
    @DisplayName("familia no encontrada → excepción capturada, no persiste CriticalDay")
    void familyNotFound_noCriticalDaySaved() {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

        service.handleCrisisSignal(String.valueOf(FAM_ID));

        verifyNoInteractions(criticalDayRepository);
    }

    @Test
    @DisplayName("flujo exitoso → CriticalDay guardado con categoría SENTINEL_ALERT y guía de AI")
    void validSignal_criticalDayPersisted() {
        Family f = family();
        AiContext ctx = mock(AiContext.class);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
        when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(ctx);
        when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
        when(aiProvider.generateResponse(any(), eq(ctx))).thenReturn("Guía de contención.");
        when(criticalDayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleCrisisSignal(String.valueOf(FAM_ID));

        verify(criticalDayRepository).save(argThat(cd ->
                FAM_ID == cd.getFamilyId()
                && "SENTINEL_ALERT".equals(cd.getCategory())
                && "SDD_CRISIS_DETECTADA".equals(cd.getEmotion())
                && "Guía de contención.".equals(cd.getAiContainmentGuide())));
    }

    @Test
    @DisplayName("AI lanza excepción → excepción capturada, CriticalDay no guardado")
    void aiThrows_noCriticalDaySaved() {
        Family f = family();
        AiContext ctx = mock(AiContext.class);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
        when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(ctx);
        when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
        when(aiProvider.generateResponse(any(), eq(ctx))).thenThrow(new RuntimeException("AI down"));

        service.handleCrisisSignal(String.valueOf(FAM_ID));

        verifyNoInteractions(criticalDayRepository);
    }

    @Test
    @DisplayName("familyId con espacios en blanco → se parsea correctamente")
    void familyIdWithWhitespace_parsedCorrectly() {
        Family f = family();
        AiContext ctx = mock(AiContext.class);
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
        when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(ctx);
        when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
        when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
        when(criticalDayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleCrisisSignal("  1  ");

        verify(familyRepository).findById(1L);
        verify(criticalDayRepository).save(any(CriticalDay.class));
    }
}
