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

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

        @Test
        @DisplayName("emoción null → prompt usa 'No especificada' y se persiste sin error")
        void crisis_emotionNull_promptUsesDefault() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("Guía OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            CriticalDay result = service.registerCrisis(FAM_ID, MEM_ID, "TENSION", "desc", null);

            assertThat(result.getEmotion()).isNull();
            verify(aiProvider).generateResponse(argThat(p -> p.contains("No especificada")), any());
        }

        @Test
        @DisplayName("WhatsApp lanza excepción → no se propaga al caller")
        void crisis_whatsappThrows_doesNotPropagateException() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("Guía OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();
            doThrow(new RuntimeException("WA down")).when(whatsAppService).sendToFamily(any(), any());

            assertThatCode(() -> service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "MIEDO"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("cascada ICF: penaliza -15 puntos desde snapshot anterior")
        void crisis_icfPenalization_snapshotSavedWithCritico() {
            Family f = family();
            RiskSnapshot prev = RiskSnapshot.builder().icf(60.0).riskLevel("MODERADO").build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(prev));
            when(riskSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publish(any());
            when(adaptivePlanService.evaluateAndProposeForFamily(FAM_ID)).thenReturn(List.of());

            service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "RABIA");

            verify(riskSnapshotRepository).save(argThat(snap ->
                    "CRITICO".equals(snap.getRiskLevel())
                    && Boolean.TRUE.equals(snap.getHasCrisis())
                    && snap.getIcf() == 45.0));   // 60 - 15 = 45
        }

        @Test
        @DisplayName("cascada ICF: floor en 10 cuando ICF previo es 20")
        void crisis_icfFloor_minimumTen() {
            Family f = family();
            RiskSnapshot prev = RiskSnapshot.builder().icf(20.0).riskLevel("CRITICO").build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(prev));
            when(riskSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publish(any());
            when(adaptivePlanService.evaluateAndProposeForFamily(FAM_ID)).thenReturn(List.of());

            service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "RABIA");

            verify(riskSnapshotRepository).save(argThat(snap -> snap.getIcf() == 10.0));  // max(10, 20-15)
        }

        @Test
        @DisplayName("sin historial de snapshots → usa ICF base 50 y penaliza a 35")
        void crisis_noSnapshotHistory_usesDefault50() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            when(riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
            when(riskSnapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publish(any());
            when(adaptivePlanService.evaluateAndProposeForFamily(FAM_ID)).thenReturn(List.of());

            service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "TRISTEZA");

            verify(riskSnapshotRepository).save(argThat(snap -> snap.getIcf() == 35.0));  // max(10, 50-15)
        }

        @Test
        @DisplayName("se publican exactamente 2 eventos: FamilyCrisisEvent + FamilyIcfRecalculatedEvent")
        void crisis_twoEventsPublished() {
            Family f = family();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(contextSynthesizer.synthesize(f, "CRISIS")).thenReturn(null);
            when(aiProvider.generateResponse(any(), any())).thenReturn("OK");
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenReturn(f);
            stubCascade();

            service.registerCrisis(FAM_ID, MEM_ID, "CONFLICTO", "desc", "RABIA");

            verify(eventPublisher, times(2)).publish(any());
        }

    // ── handleMemberCrisis ────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleMemberCrisis")
    class HandleMemberCrisis {

        @Test
        @DisplayName("lista vacía → no lanza excepción (stub vacío)")
        void emptyList_doesNotThrow() {
            assertThatCode(() -> service.handleMemberCrisis(FAM_ID, List.of(), "obs"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("lista null → no lanza excepción")
        void nullList_doesNotThrow() {
            assertThatCode(() -> service.handleMemberCrisis(FAM_ID, null, "obs"))
                    .doesNotThrowAnyException();
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
