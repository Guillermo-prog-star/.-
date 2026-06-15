package com.integrityfamily.risk.service;

import com.integrityfamily.adaptive.AdaptivePlanService;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrisisServiceImpl")
class CrisisServiceImplTest {

    @Mock CriticalDayRepository      repository;
    @Mock FamilyRepository           familyRepository;
    @Mock RiskSnapshotRepository     riskSnapshotRepository;
    @Mock AiProvider                 aiProvider;
    @Mock ContextSynthesizer         contextSynthesizer;
    @Mock WhatsAppService            whatsAppService;
    @Mock EventPublisher             eventPublisher;
    @Mock AdaptivePlanService        adaptivePlanService;
    @InjectMocks CrisisServiceImpl service;

    private static final long FAM_ID = 1L;
    private static final long MEM_ID = 10L;

    private Family family() {
        return Family.builder().id(FAM_ID).name("García").sentinelActive(false).build();
    }

    private void stubCascade() {
        when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(riskSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventPublisher).publish(any());
        when(adaptivePlanService.evaluateAndProposeForFamily(FAM_ID)).thenReturn(List.of());
    }

    // ── registerCrisis ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerCrisis")
    class RegisterCrisis {

        @Test
        @DisplayName("familia no encontrada → BusinessException NOT_FOUND")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "RABIA"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("AI responde → CriticalDay guardado con guía de contención")
        void aiSucceeds_crisisPersistedWithGuide() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("Guía de contención profesional.");
            CriticalDay saved = CriticalDay.builder().id(99L).familyId(FAM_ID).build();
            when(repository.save(any())).thenReturn(saved);
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();
            lenient().doNothing().when(whatsAppService).sendToFamily(any(), any());

            CriticalDay result = service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "RABIA");

            assertThat(result.getId()).isEqualTo(99L);
            verify(repository).save(argThat(cd ->
                    cd.getAiContainmentGuide().equals("Guía de contención profesional.")));
        }

        @Test
        @DisplayName("AI lanza excepción → guía de contención por defecto")
        void aiThrows_fallbackGuide() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenThrow(new RuntimeException("AI down"));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            CriticalDay result = service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "MIEDO");

            assertThat(result.getAiContainmentGuide()).contains("Respiración consciente");
        }

        @Test
        @DisplayName("crisis → family.sentinelActive=true y family guardada")
        void crisis_setsSentinelActive() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            service.registerCrisis(FAM_ID, MEM_ID, "TENSION", "desc", null);

            assertThat(f.getSentinelActive()).isTrue();
            verify(familyRepository).save(f);
        }

        @Test
        @DisplayName("evento de crisis publicado al EventBus")
        void crisis_eventPublished() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "TRISTEZA");

            verify(eventPublisher, atLeastOnce()).publish(any());
        }

        @Test
        @DisplayName("CriticalDay contiene categoría, emoción y memberId")
        void crisisFields_savedCorrectly() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("Guide");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            service.registerCrisis(FAM_ID, MEM_ID, "VIOLENCIA", "situación grave", "RABIA");

            verify(repository).save(argThat(cd ->
                    FAM_ID == cd.getFamilyId()
                    && MEM_ID == cd.getMemberId()
                    && "VIOLENCIA".equals(cd.getCategory())
                    && "RABIA".equals(cd.getEmotion())));
        }
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("sin crisis → lista vacía")
        void noHistory_emptyList() {
            when(repository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());

            assertThat(service.getHistory(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("con crisis → retorna lista del repo")
        void withHistory_returnsList() {
            CriticalDay cd = CriticalDay.builder().id(1L).familyId(FAM_ID).build();
            when(repository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(cd));

            assertThat(service.getHistory(FAM_ID)).hasSize(1);
        }
    }

    // ── activateProtocol ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateProtocol")
    class ActivateProtocol {

        @Test
        @DisplayName("familia no encontrada → BusinessException")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateProtocol(FAM_ID, "TEST"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("familia encontrada → sentinelActive=true, guardada")
        void familyFound_sentinelActivated() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(familyRepository.save(any())).thenReturn(f);

            service.activateProtocol(FAM_ID, "TEST");

            assertThat(f.getSentinelActive()).isTrue();
            verify(familyRepository).save(f);
        }
    }

    // ── isUnderCrisis ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isUnderCrisis")
    class IsUnderCrisis {

        @Test
        @DisplayName("familia no existe → false")
        void familyNotFound_false() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThat(service.isUnderCrisis(FAM_ID)).isFalse();
        }

        @Test
        @DisplayName("sentinelActive=null → false")
        void sentinelNull_false() {
            Family f = Family.builder().id(FAM_ID).sentinelActive(null).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));

            assertThat(service.isUnderCrisis(FAM_ID)).isFalse();
        }

        @Test
        @DisplayName("sentinelActive=false → false")
        void sentinelFalse_false() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));

            assertThat(service.isUnderCrisis(FAM_ID)).isFalse();
        }

        @Test
        @DisplayName("sentinelActive=true → true")
        void sentinelTrue_true() {
            Family f = Family.builder().id(FAM_ID).sentinelActive(true).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));

            assertThat(service.isUnderCrisis(FAM_ID)).isTrue();
        }
    }
}
