package com.integrityfamily.transformation.service;

import com.integrityfamily.transformation.domain.TransformationState;
import com.integrityfamily.transformation.domain.TransformationState.OnboardingStep;
import com.integrityfamily.transformation.domain.TransformationState.Pillar;
import com.integrityfamily.transformation.repository.TransformationStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransformationStateService")
class TransformationStateServiceTest {

    @Mock TransformationStateRepository repo;
    @InjectMocks TransformationStateService service;

    private static final long FAM_ID = 1L;

    private TransformationState defaultState() {
        return TransformationState.builder().familyId(FAM_ID).build();
    }

    // ─── getOrCreate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("estado existente → retornado directamente sin guardar")
        void existingState_returnedWithoutSave() {
            TransformationState existing = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));

            TransformationState result = service.getOrCreate(FAM_ID);

            assertThat(result).isSameAs(existing);
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("sin estado → crea y guarda nuevo con familyId")
        void noState_createsAndSaves() {
            TransformationState newState = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            when(repo.save(any())).thenReturn(newState);

            TransformationState result = service.getOrCreate(FAM_ID);

            assertThat(result).isNotNull();
            verify(repo).save(argThat(s -> FAM_ID == s.getFamilyId()));
        }
    }

    // ─── advanceOnboarding ────────────────────────────────────────────────────

    @Nested
    @DisplayName("advanceOnboarding")
    class AdvanceOnboarding {

        @Test
        @DisplayName("paso no COMPLETED → solo actualiza onboardingStep")
        void nonCompletedStep_onlySetsStep() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceOnboarding(FAM_ID, OnboardingStep.ADD_MEMBERS);

            assertThat(s.getOnboardingStep()).isEqualTo(OnboardingStep.ADD_MEMBERS);
            assertThat(s.getCurrentMonth()).isEqualTo(1); // valor por defecto intacto
        }

        @Test
        @DisplayName("paso COMPLETED → inicializa mes=1, pilar=RECONOCIMIENTO, hito=M1")
        void completedStep_initializesJourney() {
            TransformationState s = TransformationState.builder()
                    .familyId(FAM_ID).currentMonth(0).build();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceOnboarding(FAM_ID, OnboardingStep.COMPLETED);

            assertThat(s.getOnboardingStep()).isEqualTo(OnboardingStep.COMPLETED);
            assertThat(s.getCurrentMonth()).isEqualTo(1);
            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.RECONOCIMIENTO);
            assertThat(s.getMilestoneLabel()).isEqualTo("M1");
        }

        @Test
        @DisplayName("paso intermedio → month de la familia ya avanzada no se toca")
        void intermediateStep_doesNotResetMonth() {
            TransformationState s = defaultState();
            s.setCurrentMonth(15);
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceOnboarding(FAM_ID, OnboardingStep.DIAGNOSIS);

            assertThat(s.getCurrentMonth()).isEqualTo(15);
        }
    }

    // ─── advanceMonth ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("advanceMonth")
    class AdvanceMonth {

        @Test
        @DisplayName("mes=0 → IllegalArgumentException")
        void month0_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.advanceMonth(FAM_ID, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0");
        }

        @Test
        @DisplayName("mes=37 → IllegalArgumentException")
        void month37_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.advanceMonth(FAM_ID, 37))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("37");
        }

        @Test
        @DisplayName("mes=1 → pilar RECONOCIMIENTO, progreso=3%, hito=M1")
        void month1_reconocimiento_progress3() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 1);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.RECONOCIMIENTO);
            assertThat(s.getProgressPercent()).isEqualTo(3);
            assertThat(s.getMilestoneLabel()).isEqualTo("M1");
        }

        @Test
        @DisplayName("mes=6 → pilar RECONOCIMIENTO (límite del pilar)")
        void month6_reconocimiento_boundary() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 6);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.RECONOCIMIENTO);
        }

        @Test
        @DisplayName("mes=7 → pilar AMOR (primer mes del pilar)")
        void month7_amor_starts() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 7);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.AMOR);
        }

        @Test
        @DisplayName("mes=18 → pilar AMOR, progreso=50%")
        void month18_amor_progress50() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 18);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.AMOR);
            assertThat(s.getProgressPercent()).isEqualTo(50);
        }

        @Test
        @DisplayName("mes=19 → pilar ENTREGA (primer mes del pilar)")
        void month19_entrega_starts() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 19);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.ENTREGA);
        }

        @Test
        @DisplayName("mes=36 → pilar ENTREGA, progreso=100%, hito=M36")
        void month36_entrega_progress100() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 36);

            assertThat(s.getCurrentPillar()).isEqualTo(Pillar.ENTREGA);
            assertThat(s.getProgressPercent()).isEqualTo(100);
            assertThat(s.getMilestoneLabel()).isEqualTo("M36");
        }

        @Test
        @DisplayName("mes=12 → hito=M12 (formato dinámico)")
        void month12_milestoneLabel_M12() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.advanceMonth(FAM_ID, 12);

            assertThat(s.getMilestoneLabel()).isEqualTo("M12");
        }
    }

    // ─── setSprint ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setSprint")
    class SetSprint {

        @Test
        @DisplayName("actualiza el número de sprint activo")
        void updatesSprint() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.setSprint(FAM_ID, 3);

            assertThat(s.getCurrentSprintNumber()).isEqualTo(3);
            verify(repo).save(s);
        }
    }

    // ─── setActiveMission ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("setActiveMission")
    class SetActiveMission {

        @Test
        @DisplayName("actualiza la misión activa")
        void updatesActiveMission() {
            TransformationState s = defaultState();
            when(repo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(repo.save(any())).thenReturn(s);

            service.setActiveMission(FAM_ID, 42L);

            assertThat(s.getActiveMissionId()).isEqualTo(42L);
            verify(repo).save(s);
        }
    }
}
