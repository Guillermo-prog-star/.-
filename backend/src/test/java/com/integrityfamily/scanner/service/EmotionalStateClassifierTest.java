package com.integrityfamily.scanner.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.scanner.domain.EmotionalOperationalState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmotionalStateClassifier — IF-TOS")
class EmotionalStateClassifierTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private EmotionalStateClassifier classifier;

    // ── Helper ────────────────────────────────────────────────────────────────

    private static final Long FAMILY_ID = 1L;

    private Evaluation eval(double icf, String risk, int daysAgo) {
        Family family = Family.builder().id(FAMILY_ID).currentMilestone("M6").build();
        return Evaluation.builder()
                .id((long) (Math.random() * 1000))
                .family(family)
                .status(EvaluationStatus.FINALIZED)
                .icf(icf)
                .riskLevel(risk)
                .finalizedAt(LocalDateTime.now().minusDays(daysAgo))
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EMERGING")
    class Emerging {
        @Test
        void noEvaluationsYieldsEmerging() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID)).thenReturn(List.of());
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.EMERGING);
        }

        @Test
        void singleNonCriticalEvaluationYieldsEmerging() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(65, "MODERADO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.EMERGING);
        }
    }

    @Nested
    @DisplayName("CRITICAL")
    class Critical {
        @Test
        void latestEvaluationCriticoYieldsCritical() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(75, "BAJO", 5), eval(15, "CRITICO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.CRITICAL);
        }
    }

    @Nested
    @DisplayName("RESOLVED")
    class Resolved {
        @Test
        void twoBajoWithIcfAbove70YieldsResolved() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(72, "BAJO", 10), eval(75, "BAJO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.RESOLVED);
        }

        @Test
        void twoBajoButIcfBelow70DoesNotYieldResolved() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(60, "BAJO", 10), eval(65, "BAJO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isNotEqualTo(EmotionalOperationalState.RESOLVED);
        }
    }

    @Nested
    @DisplayName("RECOVERING")
    class Recovering {
        @Test
        void improvingFromAltoYieldsRecovering() {
            // Prev: ALTO con ICF 30 → Latest: ICF 45 (mejora de 15 > threshold 5)
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(30, "ALTO", 10), eval(45, "MODERADO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.RECOVERING);
        }

        @Test
        void improvingFromCriticoYieldsRecovering() {
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(10, "CRITICO", 10), eval(30, "ALTO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.RECOVERING);
        }
    }

    @Nested
    @DisplayName("ESCALATING")
    class Escalating {
        @Test
        void threeStepDeclineYieldsEscalating() {
            // 3 evaluaciones con caída progresiva (>3 pts cada paso)
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(70, "BAJO", 20), eval(60, "MODERADO", 10), eval(50, "MODERADO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.ESCALATING);
        }

        @Test
        void significantDropWithAltoRiskYieldsEscalating() {
            // Caída > 5 puntos con riesgo ALTO
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(55, "MODERADO", 10), eval(45, "ALTO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.ESCALATING);
        }
    }

    @Nested
    @DisplayName("STABLE")
    class Stable {
        @Test
        void noSignificantChangeYieldsStable() {
            // Dos evaluaciones con cambio pequeño y riesgo MODERADO
            when(evaluationRepository.findByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(eval(55, "MODERADO", 10), eval(57, "MODERADO", 0)));
            assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.STABLE);
        }
    }

    @Test
    @DisplayName("Evaluaciones no-FINALIZED son ignoradas")
    void nonFinalizedEvaluationsAreIgnored() {
        Family family = Family.builder().id(FAMILY_ID).currentMilestone("M6").build();
        Evaluation inProgress = Evaluation.builder()
                .id(99L).family(family)
                .status(EvaluationStatus.IN_PROGRESS)
                .icf(10.0).riskLevel("CRITICO")
                .finalizedAt(null)
                .build();
        when(evaluationRepository.findByFamilyId(FAMILY_ID)).thenReturn(List.of(inProgress));
        assertThat(classifier.classify(FAMILY_ID)).isEqualTo(EmotionalOperationalState.EMERGING);
    }
}
