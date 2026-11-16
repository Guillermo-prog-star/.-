package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.interop.canonical.Assessment;
import com.integrityfamily.interop.canonical.Observation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssessmentMapper")
class AssessmentMapperTest {

    private Evaluation baseEvaluation(Family family) {
        return Evaluation.builder()
                .id(100L)
                .family(family)
                .status(EvaluationStatus.FINALIZED)
                .startedAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                .finalizedAt(LocalDateTime.of(2026, 7, 1, 10, 30))
                .icf(72.5)
                .riskLevel("MODERATE")
                .criticalDimension("comunicacion")
                .build();
    }

    @Test
    @DisplayName("mapea campos base y agrega ICF como Observation")
    void shouldMapBaseFields() {
        Family family = Family.builder().id(1L).build();
        Evaluation evaluation = baseEvaluation(family);

        Assessment assessment = AssessmentMapper.toCanonical(evaluation, List.of());

        assertThat(assessment.canonicalId()).isEqualTo("evaluation-100");
        assertThat(assessment.subjectId()).isEqualTo("family-1");
        assertThat(assessment.status()).isEqualTo("FINALIZED");
        assertThat(assessment.overallRiskLevel()).isEqualTo("MODERATE");
        assertThat(assessment.criticalDimension()).isEqualTo("comunicacion");

        Observation icf = assessment.observations().stream()
                .filter(o -> o.code().equals("ICF")).findFirst().orElseThrow();
        assertThat(icf.valueNumeric()).isEqualTo(72.5);
        assertThat(icf.status()).isEqualTo("FINAL");
    }

    @Test
    @DisplayName("campos neurofenomenológicos null → no generan Observation (evita ruido)")
    void shouldSkipNullVectorFields() {
        Family family = Family.builder().id(1L).build();
        Evaluation evaluation = baseEvaluation(family);

        Assessment assessment = AssessmentMapper.toCanonical(evaluation, List.of());

        assertThat(assessment.observations().stream().anyMatch(o -> o.code().equals("SOMATIC_AWARENESS"))).isFalse();
    }

    @Test
    @DisplayName("dimensionScores se mapean cada uno como Observation propia")
    void shouldMapDimensionScores() {
        Family family = Family.builder().id(1L).build();
        Evaluation evaluation = baseEvaluation(family);

        EvaluationDimensionScore d1 = EvaluationDimensionScore.builder().id(1L).dimensionName("emociones").score(80.0).build();
        EvaluationDimensionScore d2 = EvaluationDimensionScore.builder().id(2L).dimensionName("habitos").score(60.0).build();

        Assessment assessment = AssessmentMapper.toCanonical(evaluation, List.of(d1, d2));

        assertThat(assessment.observations().stream().filter(o -> o.code().equals("emociones")).findFirst().orElseThrow().valueNumeric())
                .isEqualTo(80.0);
        assertThat(assessment.observations().stream().filter(o -> o.code().equals("habitos")).findFirst().orElseThrow().valueNumeric())
                .isEqualTo(60.0);
    }

    @Test
    @DisplayName("evaluación sin finalizedAt → status PRELIMINARY")
    void shouldMarkPreliminary_whenNotFinalized() {
        Family family = Family.builder().id(1L).build();
        Evaluation evaluation = Evaluation.builder()
                .id(101L).family(family).status(EvaluationStatus.STARTED)
                .startedAt(LocalDateTime.now()).icf(50.0).build();

        Assessment assessment = AssessmentMapper.toCanonical(evaluation, List.of());

        Observation icf = assessment.observations().get(0);
        assertThat(icf.status()).isEqualTo("PRELIMINARY");
    }
}
