package com.integrityfamily.reports.service;

import com.integrityfamily.analytics.service.SentimentAnalyticsService;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.repository.AuditEventRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfExportService")
class PdfExportServiceTest {

    @Mock ReportService                 reportService;
    @Mock SentimentAnalyticsService     sentimentAnalyticsService;
    @Mock AdminAlertRepository          adminAlertRepository;
    @Mock FamilyRepository              familyRepository;
    @Mock EvaluationRepository          evaluationRepository;
    @Mock UserRepository                userRepository;
    @Mock AuditEventRepository          auditEventRepository;
    @Mock InferenceRecordRepository     inferenceRecordRepository;
    @Mock FamilyAlertRepository         familyAlertRepository;

    @InjectMocks PdfExportService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReportService.ConsolidatedReport emptyConsolidatedReport() {
        return ReportService.ConsolidatedReport.builder()
                .reportId("REP-0")
                .metadata(Map.of("total_familias", 0))
                .consolidadoDimensiones(Collections.emptyMap())
                .casosAltoRiesgo(Collections.emptyList())
                .build();
    }

    private SentimentAnalyticsService.SentimentReport emptySentimentReport() {
        return SentimentAnalyticsService.SentimentReport.builder()
                .totalComments(0)
                .aiExecutiveSummary("Sin comentarios aún.")
                .sentimentDistribution(Collections.emptyMap())
                .dimensionScores(Collections.emptyMap())
                .build();
    }

    // ── generateConsolidatedPdf ───────────────────────────────────────────────

    @Test
    @DisplayName("generateConsolidatedPdf genera un PDF válido (header %PDF) con datos mínimos")
    void generateConsolidatedPdf_minimalData_returnsPdf() {
        when(reportService.generateConsolidatedReport()).thenReturn(emptyConsolidatedReport());
        when(sentimentAnalyticsService.analyzeGlobalFeedback()).thenReturn(emptySentimentReport());
        when(adminAlertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        byte[] pdf = service.generateConsolidatedPdf();

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1_000);
    }

    // ── generateFamilyEvolutivePdf — familia no encontrada ───────────────────

    @Test
    @DisplayName("generateFamilyEvolutivePdf lanza RuntimeException cuando la familia no existe")
    void generateFamilyEvolutivePdf_familyNotFound_throwsException() {
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateFamilyEvolutivePdf(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── generateFamilyEvolutivePdf — sin evaluación finalizada ───────────────

    @Test
    @DisplayName("generateFamilyEvolutivePdf sin evaluación finalizada genera un PDF válido")
    void generateFamilyEvolutivePdf_noFinishedEval_returnsPdf() {
        Family family = Family.builder()
                .id(1L)
                .name("Los García")
                .familyCode("IF-CO-001")
                .currentMilestone("W1")
                .build();
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(
                eq(1L), eq(EvaluationStatus.FINALIZED))).thenReturn(Optional.empty());
        when(userRepository.findByFamilyId(1L)).thenReturn(List.of());
        when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of());
        when(inferenceRecordRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(familyAlertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        byte[] pdf = service.generateFamilyEvolutivePdf(1L);

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(1_000);
    }
}
