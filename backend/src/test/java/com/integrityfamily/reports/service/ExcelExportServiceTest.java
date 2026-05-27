package com.integrityfamily.reports.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelExportService")
class ExcelExportServiceTest {

    @Mock ReportService reportService;

    @InjectMocks ExcelExportService service;

    @Test
    @DisplayName("retorna byte array no vacio con reporte sin casos criticos")
    void generateConsolidatedExcel_emptyReport_returnsNonEmptyByteArray() throws IOException {
        ReportService.ConsolidatedReport report = ReportService.ConsolidatedReport.builder()
                .reportId("REP-TEST-001")
                .metadata(Map.of("total_familias", 0))
                .consolidadoDimensiones(Collections.emptyMap())
                .casosAltoRiesgo(Collections.emptyList())
                .build();
        when(reportService.generateConsolidatedReport()).thenReturn(report);

        byte[] result = service.generateConsolidatedExcel();

        assertThat(result).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("retorna byte array no vacio con casos de alto riesgo incluidos")
    void generateConsolidatedExcel_withCriticalCases_returnsNonEmptyByteArray() throws IOException {
        ReportService.DimensionSummary dimSummary = ReportService.DimensionSummary.builder()
                .promedioScore(45.0)
                .nivelAlerta("ALTO")
                .build();

        ReportService.CaseRegistry criticalCase = ReportService.CaseRegistry.builder()
                .familiaId("FAM-001")
                .puntuacionTotal(35.0)   // < 50, se resaltara en rojo
                .dimensionCritica("comunicacion")
                .impactoDelta("-15%")
                .build();

        ReportService.ConsolidatedReport report = ReportService.ConsolidatedReport.builder()
                .reportId("REP-TEST-002")
                .metadata(Map.of("total_familias", 5))
                .consolidadoDimensiones(Map.of("comunicacion", dimSummary))
                .casosAltoRiesgo(java.util.List.of(criticalCase))
                .build();
        when(reportService.generateConsolidatedReport()).thenReturn(report);

        byte[] result = service.generateConsolidatedExcel();

        // El xlsx generado siempre supera los 3 KB
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(3_000);
    }
}
