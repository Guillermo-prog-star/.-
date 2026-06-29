package com.integrityfamily.family.service;

import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyJourneyResponse;
import com.integrityfamily.family.dto.FamilyJourneyResponse.JourneyStatus;
import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("FamilyJourneyService — Unit Tests")
class FamilyJourneyServiceTest {

    @Mock FamilyRepository              familyRepository;
    @Mock MemberRepository              memberRepository;
    @Mock FamilyIdentityProfileRepository identityRepository;
    @Mock FamilyDnaRepository           dnaRepository;
    @Mock EvaluationRepository          evaluationRepository;
    @Mock ImprovementPlanRepository     planRepository;
    @Mock PlanTaskRepository            planTaskRepository;
    @Mock FamilySprintRepository        sprintRepository;
    @Mock SprintDailyRepository         dailyRepository;
    @Mock TaskEvidenceRepository        evidenceRepository;
    @Mock AiInferenceRepository         aiInferenceRepository;
    @Mock FamilyLegacyRepository        legacyRepository;
    @Mock FamilyDocumentaryRepository   documentaryRepository;

    @InjectMocks FamilyJourneyService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia López");
        // stubFamilyFound() se llama explícitamente en cada test (no aquí, para evitar stubbings innecesarios)
    }

    private void stubFamilyFound() {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Configura todos los mocks para que la familia esté en nivel 0 (solo plataforma). */
    private void stubLevel0() {
        stubFamilyFound();
        when(identityRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
        when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(0L);
        when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID)).thenReturn(List.of());
        when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
        when(planTaskRepository.countByFamilyId(FAM_ID)).thenReturn(0L);
        when(sprintRepository.countByFamilyId(FAM_ID)).thenReturn(0L);
        when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());
        when(aiInferenceRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(legacyRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
        when(documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
    }

    private Evaluation finalizedEval(double icf) {
        Evaluation e = new Evaluation();
        e.setStatus(EvaluationStatus.FINALIZED);
        e.setIcf(icf);
        return e;
    }

    private FamilyLegacy legacyWith(String lessons) {
        FamilyLegacy l = new FamilyLegacy();
        l.setHistoryLessons(lessons);
        return l;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate() — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando la familia no existe")
        void throwsWhenFamilyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.evaluate(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate() — nivel 0 (solo plataforma)")
    class Level0 {

        @BeforeEach
        void setup() { stubLevel0(); }

        @Test
        @DisplayName("devuelve nivel 0 cuando no hay datos")
        void currentLevelIsZero() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.currentLevel()).isEqualTo(0);
        }

        @Test
        @DisplayName("devuelve journeyProgress de ~7% (1 de 14 niveles)")
        void progressIsSevenPercent() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            // ceil(1/14 * 100) = 7
            assertThat(r.journeyProgress()).isBetween(7, 8);
        }

        @Test
        @DisplayName("el nivel 0 (Plataforma) es COMPLETE")
        void level0IsComplete() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(0).status()).isEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("el nivel 1 (Identidad) es NEXT")
        void level1IsNext() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(1).status()).isEqualTo(JourneyStatus.NEXT);
        }

        @Test
        @DisplayName("los niveles 3+ están LOCKED")
        void level3PlusAreLocked() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            for (int i = 3; i <= 13; i++) {
                assertThat(r.levels().get(i).status())
                    .as("nivel %d debería estar LOCKED", i)
                    .isNotEqualTo(JourneyStatus.COMPLETE);
            }
        }

        @Test
        @DisplayName("nextLevel apunta al nivel 1")
        void nextLevelIsOne() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.nextLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("nextAction describe completar el perfil de identidad")
        void nextActionDescribesIdentity() {
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.nextAction()).containsIgnoringCase("identidad");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate() — progresión de niveles individuales")
    class LevelProgression {

        @Test
        @DisplayName("nivel 1 completo cuando familia tiene nombre e identityProfile")
        void level1CompleteWithProfile() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(1).status()).isEqualTo(JourneyStatus.COMPLETE);
            assertThat(r.currentLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("nivel 1 incompleto cuando la familia no tiene nombre")
        void level1IncompleteWithoutName() {
            stubLevel0();
            family.setName(""); // nombre vacío
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(1).status()).isNotEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 2 completo cuando hay al menos 2 miembros")
        void level2CompleteWithTwoMembers() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(2L);

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(2).status()).isEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 2 incompleto con solo 1 miembro")
        void level2IncompleteWithOneMember() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(1L);

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(2).status()).isNotEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 3 completo cuando familia tiene guardianMemberId")
        void level3CompleteWithGuardian() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(2L);
            family.setGuardianMemberId(42L);

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(3).status()).isEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 5 completo con al menos una evaluación FINALIZED con ICF")
        void level5CompleteWithFinalizedEval() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(2L);
            family.setGuardianMemberId(42L);
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(new com.integrityfamily.dna.domain.FamilyDna()));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                .thenReturn(List.of(finalizedEval(72.0)));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(5).status()).isEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 12 (Consejo) completo cuando legado tiene historyLessons")
        void level12CompleteWithLessons() {
            stubLevel0();
            when(identityRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(2L);
            family.setGuardianMemberId(42L);
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(new com.integrityfamily.dna.domain.FamilyDna()));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                .thenReturn(List.of(finalizedEval(72.0)));
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(new ImprovementPlan()));
            when(planTaskRepository.countByFamilyId(FAM_ID)).thenReturn(3L);
            when(sprintRepository.countByFamilyId(FAM_ID)).thenReturn(1L);
            FamilySprint sprint = new FamilySprint(); sprint.setId(10L);
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(sprint));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(List.of(new SprintDaily()));
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(new TaskEvidence()));
            when(aiInferenceRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(new AiInferenceEntity()));
            when(legacyRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(legacyWith("Aprendimos que la comunicación es clave.")));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(12).status()).isEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 12 no completo cuando historyLessons está vacío")
        void level12IncompleteWithEmptyLessons() {
            stubLevel0();
            when(legacyRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(legacyWith("")));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(12).status()).isNotEqualTo(JourneyStatus.COMPLETE);
        }

        @Test
        @DisplayName("nivel 13 (Legado) completo cuando hay documental aunque no haya legacy entity")
        void level13CompleteWithDocumentaryOnly() {
            stubLevel0();
            when(legacyRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            when(documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(List.of(new FamilyDocumentary()));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels().get(13).status()).isEqualTo(JourneyStatus.COMPLETE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("evaluate() — cálculo de progreso")
    class ProgressCalculation {

        @Test
        @DisplayName("progress 100% cuando todos los 14 niveles están completos")
        void fullProgressWhenAllComplete() {
            stubFamilyFound();
            family.setGuardianMemberId(42L);
            when(identityRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(new FamilyIdentityProfile()));
            when(memberRepository.countByFamilyId(FAM_ID)).thenReturn(3L);
            when(dnaRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(new com.integrityfamily.dna.domain.FamilyDna()));
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                .thenReturn(List.of(finalizedEval(80.0)));
            when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(new ImprovementPlan()));
            when(planTaskRepository.countByFamilyId(FAM_ID)).thenReturn(2L);
            when(sprintRepository.countByFamilyId(FAM_ID)).thenReturn(1L);
            FamilySprint sprint = new FamilySprint(); sprint.setId(5L);
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(sprint));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(5L)).thenReturn(List.of(new SprintDaily()));
            when(evidenceRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(new TaskEvidence()));
            when(aiInferenceRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of(new AiInferenceEntity()));
            when(legacyRepository.findByFamilyId(FAM_ID))
                .thenReturn(Optional.of(legacyWith("Lección importante.")));
            when(documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(List.of(new FamilyDocumentary()));

            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.journeyProgress()).isEqualTo(100);
        }

        @Test
        @DisplayName("response incluye familyId y familyName correctos")
        void responseMetadata() {
            stubLevel0();
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.familyId()).isEqualTo(FAM_ID);
            assertThat(r.familyName()).isEqualTo("Familia López");
        }

        @Test
        @DisplayName("response incluye exactamente 14 niveles")
        void responseHas14Levels() {
            stubLevel0();
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            assertThat(r.levels()).hasSize(14);
        }

        @Test
        @DisplayName("cada nivel tiene número secuencial 0-13")
        void levelsAreSequential() {
            stubLevel0();
            FamilyJourneyResponse r = service.evaluate(FAM_ID);
            for (int i = 0; i <= 13; i++) {
                assertThat(r.levels().get(i).level()).isEqualTo(i);
            }
        }
    }
}
