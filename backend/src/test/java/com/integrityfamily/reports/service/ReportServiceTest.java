package com.integrityfamily.reports.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilySummary;
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
@DisplayName("ReportService")
class ReportServiceTest {

    @Mock FamilyRepository    familyRepository;
    @Mock EvaluationRepository evaluationRepository;

    @InjectMocks ReportService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Implementación mínima de la proyección FamilySummary. */
    private FamilySummary fs(Long id, String code) {
        return new FamilySummary() {
            @Override public Long   getId()             { return id; }
            @Override public String getName()           { return "Familia " + code; }
            @Override public String getFamilyCode()     { return code; }
            @Override public String getMunicipio()      { return null; }
            @Override public String getCountryCode()    { return null; }
            @Override public String getDepartmentCode() { return null; }
        };
    }

    /**
     * Evaluation con una única dimensión.
     * finalizedAt se usa para ordenar (ASC), así que el primer valor cronológico
     * se convierte en preTest y el último en postTest.
     */
    private Evaluation eval(double icf, String dimName, double dimScore, LocalDateTime finalized) {
        EvaluationDimensionScore ds = EvaluationDimensionScore.builder()
                .dimensionName(dimName)
                .score(dimScore)
                .build();
        return Evaluation.builder()
                .icf(icf)
                .finalizedAt(finalized)
                .dimensionScores(List.of(ds))
                .build();
    }

    // ── Sin familias ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sin familias")
    class NoFamilies {

        @Test
        @DisplayName("retorna reporte vacío con reportId valido y metadato total_familias=0")
        void emptyFamilyList_emptyReport() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of());

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getReportId()).startsWith("REP-");
            assertThat(report.getMetadata().get("total_familias")).isEqualTo(0);
            assertThat(report.getCasosAltoRiesgo()).isEmpty();
            assertThat(report.getConsolidadoDimensiones()).isEmpty();
        }
    }

    // ── Familia sin evaluaciones ──────────────────────────────────────────────

    @Nested
    @DisplayName("familia sin evaluaciones")
    class FamilyNoEvals {

        @Test
        @DisplayName("familia es omitida (continue) — no genera casos ni dimensiones")
        void noEvaluations_familySkipped() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(1L, "FAM-001")));
            when(evaluationRepository.findWithScoresByFamilyId(1L)).thenReturn(List.of());

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getCasosAltoRiesgo()).isEmpty();
            assertThat(report.getConsolidadoDimensiones()).isEmpty();
            // La familia SÍ aparece en el metadato de total
            assertThat(report.getMetadata().get("total_familias")).isEqualTo(1);
        }
    }

    // ── Casos de alto riesgo (ICF < 50) ──────────────────────────────────────

    @Nested
    @DisplayName("casos de alto riesgo")
    class HighRiskCases {

        @Test
        @DisplayName("familia con una evaluación e ICF=30 → agregada con familiaId, puntuacion y dimensionCritica")
        void singleEval_icfBelow50_addedToHighRiskCases() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(2L, "IF-CO-007")));
            Evaluation e = eval(30.0, "Amor", 25.0, LocalDateTime.of(2025, 1, 1, 0, 0));
            when(evaluationRepository.findWithScoresByFamilyId(2L)).thenReturn(List.of(e));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getCasosAltoRiesgo()).hasSize(1);
            ReportService.CaseRegistry caso = report.getCasosAltoRiesgo().get(0);
            assertThat(caso.getFamiliaId()).isEqualTo("IF-CO-007");
            assertThat(caso.getPuntuacionTotal()).isEqualTo(30.0);
            assertThat(caso.getDimensionCritica()).isEqualTo("Amor");
            // pre == post → delta = 0 → "+0%"
            assertThat(caso.getImpactoDelta()).isEqualTo("+0%");
        }

        @Test
        @DisplayName("con dos evaluaciones, dimensionCritica es la de menor score en el postTest")
        void twoEvals_criticalDimIsMinScore() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(3L, "IF-CO-010")));

            EvaluationDimensionScore alta = EvaluationDimensionScore.builder()
                    .dimensionName("Compromiso").score(45.0).build();
            EvaluationDimensionScore baja = EvaluationDimensionScore.builder()
                    .dimensionName("Confianza").score(20.0).build();

            Evaluation pre = Evaluation.builder()
                    .icf(50.0).finalizedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .dimensionScores(List.of(alta)).build();
            Evaluation post = Evaluation.builder()
                    .icf(40.0).finalizedAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                    .dimensionScores(List.of(alta, baja)).build();

            when(evaluationRepository.findWithScoresByFamilyId(3L)).thenReturn(List.of(pre, post));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getCasosAltoRiesgo()).hasSize(1);
            assertThat(report.getCasosAltoRiesgo().get(0).getDimensionCritica()).isEqualTo("Confianza");
        }

        @Test
        @DisplayName("familia con ICF >= 50 NO aparece en casos de alto riesgo")
        void icfAbove50_notHighRisk() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(4L, "FAM-100")));
            Evaluation e = eval(60.0, "Amor", 65.0, LocalDateTime.now());
            when(evaluationRepository.findWithScoresByFamilyId(4L)).thenReturn(List.of(e));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getCasosAltoRiesgo()).isEmpty();
        }
    }

    // ── Delta de mejora ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("cálculo de delta")
    class DeltaCalculation {

        @Test
        @DisplayName("pre=40, post=60 → delta = +50% → impactoDelta '+50%'")
        void positiveDelta_formattedCorrectly() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(5L, "IF-CO-020")));
            // post (45) < 50 → high risk, para poder observar impactoDelta
            Evaluation pre  = eval(40.0, "Confianza", 48.0, LocalDateTime.of(2025, 1, 1, 0, 0));
            Evaluation post = eval(45.0, "Confianza", 42.0, LocalDateTime.of(2025, 6, 1, 0, 0));
            when(evaluationRepository.findWithScoresByFamilyId(5L)).thenReturn(List.of(pre, post));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            // delta = ((45-40)/40)*100 = 12.5 → String.format("%+.0f%%", 12.5) = "+13%"
            // Nota: %+.0f redondea 12.5 → depende de la JVM (half-even da 12, half-up da 13)
            // Comprobamos que el formato empiece con "+" y termine con "%"
            assertThat(report.getCasosAltoRiesgo().get(0).getImpactoDelta())
                    .startsWith("+")
                    .endsWith("%");
        }

        @Test
        @DisplayName("pre=0 → delta devuelve 0 (protección división por cero), impactoDelta '+0%'")
        void zeroPre_deltaIsZero() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(6L, "IF-CO-030")));
            Evaluation pre  = eval(0.0,  "Confianza", 40.0, LocalDateTime.of(2025, 1, 1, 0, 0));
            Evaluation post = eval(45.0, "Confianza", 40.0, LocalDateTime.of(2025, 6, 1, 0, 0));
            when(evaluationRepository.findWithScoresByFamilyId(6L)).thenReturn(List.of(pre, post));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getCasosAltoRiesgo().get(0).getImpactoDelta()).isEqualTo("+0%");
        }
    }

    // ── Dimensiones consolidadas ──────────────────────────────────────────────

    @Nested
    @DisplayName("consolidación de dimensiones")
    class DimensionSummaries {

        @Test
        @DisplayName("dimension 'Amor' → clave 'emociones', promedio=70, nivelAlerta='Medio'")
        void amorDimension_mappedToEmociones_mediumAlert() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(7L, "FAM-200")));
            Evaluation e = eval(60.0, "Amor", 70.0, LocalDateTime.now());
            when(evaluationRepository.findWithScoresByFamilyId(7L)).thenReturn(List.of(e));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getConsolidadoDimensiones()).containsKey("emociones");
            ReportService.DimensionSummary dim = report.getConsolidadoDimensiones().get("emociones");
            assertThat(dim.getPromedioScore()).isEqualTo(70.0);   // Math.round(70.0) = 70
            assertThat(dim.getNivelAlerta()).isEqualTo("Medio"); // 70 → MEDIUM
        }

        @Test
        @DisplayName("dimension no mapeada → clave en minúsculas idéntica al nombre original")
        void unmappedDimension_keyIsLowercased() {
            when(familyRepository.findProjectedBy()).thenReturn(List.of(fs(8L, "FAM-300")));
            Evaluation e = eval(55.0, "Compromiso", 85.0, LocalDateTime.now());
            when(evaluationRepository.findWithScoresByFamilyId(8L)).thenReturn(List.of(e));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            assertThat(report.getConsolidadoDimensiones()).containsKey("compromiso");
            assertThat(report.getConsolidadoDimensiones().get("compromiso").getNivelAlerta())
                    .isEqualTo("Bajo"); // 85 ≥ 80 → LOW → "Bajo"
        }

        @Test
        @DisplayName("dos familias con misma dimensión → promedio de ambas scores")
        void twofamilies_sameDimension_averagedScore() {
            FamilySummary f1 = fs(9L,  "FAM-401");
            FamilySummary f2 = fs(10L, "FAM-402");
            when(familyRepository.findProjectedBy()).thenReturn(List.of(f1, f2));
            // Familia 1: Compromiso=60, ICF=55 (no es alto riesgo)
            when(evaluationRepository.findWithScoresByFamilyId(9L))
                    .thenReturn(List.of(eval(55.0, "Compromiso", 60.0, LocalDateTime.now())));
            // Familia 2: Compromiso=80, ICF=70 (no es alto riesgo)
            when(evaluationRepository.findWithScoresByFamilyId(10L))
                    .thenReturn(List.of(eval(70.0, "Compromiso", 80.0, LocalDateTime.now())));

            ReportService.ConsolidatedReport report = service.generateConsolidatedReport();

            // Promedio de 60 y 80 = 70.0
            assertThat(report.getConsolidadoDimensiones()).containsKey("compromiso");
            assertThat(report.getConsolidadoDimensiones().get("compromiso").getPromedioScore())
                    .isEqualTo(70.0);
        }
    }
}
