package com.integrityfamily.guardian.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.PromptGenerator;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.guardian.dto.GuardianBriefingResponse;
import com.integrityfamily.participation.service.ParticipationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianBriefingService")
class GuardianBriefingServiceTest {

    @Mock FamilyRepository           familyRepository;
    @Mock ImprovementPlanRepository  planRepository;
    @Mock ParticipationService       participationService;
    @Mock PromptGenerator            promptGenerator;
    @Mock AiProvider                 aiProvider;
    @InjectMocks GuardianBriefingService service;

    private static final long FAM_ID      = 1L;
    private static final long GUARDIAN_ID = 10L;
    private static final long MEMBER_ID   = 20L;

    private ParticipationService.FamilyParticipationSummary emptySummary() {
        return new ParticipationService.FamilyParticipationSummary(0, 0, 0, List.of(), "NONE");
    }

    private ParticipationService.FamilyParticipationSummary summaryWith(
            List<ParticipationService.MemberActivity> activities, int active, int inactive, String fatigue) {
        return new ParticipationService.FamilyParticipationSummary(
                activities.size(), active, inactive, activities, fatigue);
    }

    private ParticipationService.MemberActivity activity(long id, String name, boolean activeThisWeek, long days) {
        return new ParticipationService.MemberActivity(id, name, LocalDateTime.now().minusDays(days), days, activeThisWeek);
    }

    // ── getBriefing ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBriefing")
    class GetBriefing {

        @Test
        @DisplayName("familia no encontrada → BusinessException FAMILY_NOT_FOUND")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBriefing(FAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("guardianMemberId no coincide con ningún miembro → guardianName='Guardián'")
        void noMatchingGuardian_defaultName() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(999L).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, 999L)).thenReturn(emptySummary());
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(promptGenerator.buildGuardianBriefingPrompt(any(), any(), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("prompt");
            when(aiProvider.generateWithFullPrompt("prompt")).thenReturn("OK");

            GuardianBriefingResponse resp = service.getBriefing(FAM_ID);

            assertThat(resp.guardianName()).isEqualTo("Guardián");
        }

        @Test
        @DisplayName("guardián encontrado en miembros → usa su nombre real")
        void guardianFound_usesRealName() {
            FamilyMember guardian = FamilyMember.builder().id(GUARDIAN_ID).fullName("Ana García").build();
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            f.getMembers().add(guardian);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(emptySummary());
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(promptGenerator.buildGuardianBriefingPrompt(eq("Ana García"), any(), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenReturn("OK");

            GuardianBriefingResponse resp = service.getBriefing(FAM_ID);

            assertThat(resp.guardianName()).isEqualTo("Ana García");
        }

        @Test
        @DisplayName("sin planes → completionRate=0.0")
        void noPlans_completionRateZero() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(emptySummary());
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(promptGenerator.buildGuardianBriefingPrompt(any(), any(), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenReturn("OK");

            assertThat(service.getBriefing(FAM_ID).planCompletionRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("plan con 2 tareas, 1 completada → completionRate=0.5")
        void planWithHalfCompleted_rate05() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            PlanTask done = PlanTask.builder().completed(true).build();
            PlanTask pending = PlanTask.builder().completed(false).build();
            ImprovementPlan plan = ImprovementPlan.builder().build();
            plan.getTasks().add(done);
            plan.getTasks().add(pending);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(emptySummary());
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(plan));
            when(promptGenerator.buildGuardianBriefingPrompt(any(), any(), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenReturn("OK");

            assertThat(service.getBriefing(FAM_ID).planCompletionRate()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("aiProvider lanza excepción → mensaje fallback con conteo de inactivos")
        void aiProviderThrows_fallbackMessage() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            ParticipationService.FamilyParticipationSummary summary =
                    summaryWith(List.of(), 1, 2, "NONE");
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(summary);
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(promptGenerator.buildGuardianBriefingPrompt(any(), any(), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenThrow(new RuntimeException("API error"));

            GuardianBriefingResponse resp = service.getBriefing(FAM_ID);

            assertThat(resp.aiMessage()).contains("2");
        }

        @Test
        @DisplayName("fatigueSignal propagada desde participationService")
        void fatigueSignalPropagated() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            ParticipationService.FamilyParticipationSummary summary =
                    summaryWith(List.of(), 1, 0, "HIGH");
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(summary);
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
            when(promptGenerator.buildGuardianBriefingPrompt(any(), eq("HIGH"), anyInt(), anyInt(), any(), any(), anyLong(), any(), anyDouble()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenReturn("OK");

            assertThat(service.getBriefing(FAM_ID).fatigueSignal()).isEqualTo("HIGH");
        }
    }

    // ── generateReengagementMessage ───────────────────────────────────────────

    @Nested
    @DisplayName("generateReengagementMessage")
    class GenerateReengagementMessage {

        @Test
        @DisplayName("familia no encontrada → BusinessException")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateReengagementMessage(FAM_ID, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("miembro objetivo no existe en familia → BusinessException MEMBER_NOT_FOUND")
        void targetMemberNotFound_throws() {
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));

            assertThatThrownBy(() -> service.generateReengagementMessage(FAM_ID, MEMBER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Miembro no encontrado");
        }

        @Test
        @DisplayName("éxito → llama a promptGenerator y aiProvider; retorna mensaje")
        void success_callsAiAndReturns() {
            FamilyMember target = FamilyMember.builder().id(MEMBER_ID).fullName("Luis").build();
            Family f = Family.builder().id(FAM_ID).name("García").guardianMemberId(GUARDIAN_ID).build();
            f.getMembers().add(target);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID)).thenReturn(
                    summaryWith(List.of(activity(MEMBER_ID, "Luis", false, 5)), 0, 1, "NONE"));
            when(promptGenerator.buildReengagementPrompt("el Guardián", "Luis", 5L, "García"))
                    .thenReturn("re-prompt");
            when(aiProvider.generateWithFullPrompt("re-prompt")).thenReturn("¡Hola Luis!");

            String result = service.generateReengagementMessage(FAM_ID, MEMBER_ID);

            assertThat(result).isEqualTo("¡Hola Luis!");
        }

        @Test
        @DisplayName("aiProvider lanza excepción → fallback con nombre del miembro")
        void aiProviderThrows_fallbackWithMemberName() {
            FamilyMember target = FamilyMember.builder().id(MEMBER_ID).fullName("Carlos").build();
            Family f = Family.builder().id(FAM_ID).name("Test").guardianMemberId(GUARDIAN_ID).build();
            f.getMembers().add(target);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(f));
            when(participationService.getSummary(FAM_ID, GUARDIAN_ID))
                    .thenReturn(summaryWith(List.of(activity(MEMBER_ID, "Carlos", false, 8)), 0, 1, "NONE"));
            when(promptGenerator.buildReengagementPrompt(any(), eq("Carlos"), anyLong(), any()))
                    .thenReturn("p");
            when(aiProvider.generateWithFullPrompt("p")).thenThrow(new RuntimeException("AI down"));

            String result = service.generateReengagementMessage(FAM_ID, MEMBER_ID);

            assertThat(result).contains("Carlos");
        }
    }
}
