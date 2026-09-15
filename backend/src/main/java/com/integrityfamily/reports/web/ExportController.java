package com.integrityfamily.reports.web;

import com.integrityfamily.reports.service.ReportService;
import com.integrityfamily.reports.service.ExcelExportService;
import com.integrityfamily.reports.service.PdfExportService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.security.SecurityValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ExportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;
    private final SecurityValidator securityValidator;

    @GetMapping("/consolidated")
    public ApiResponse<ReportService.ConsolidatedReport> getConsolidated() {
        return ApiResponse.ok(reportService.generateConsolidatedReport());
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        byte[] data = excelExportService.generateConsolidatedExcel();
        String filename = "IFE_Reporte_Consolidado_Alfa_" + System.currentTimeMillis() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] data = pdfExportService.generateConsolidatedPdf();
        String filename = "IFE_Dashboard_Visual_Alfa_" + System.currentTimeMillis() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/export/pdf/family/{familyId}")
    public ResponseEntity<byte[]> exportFamilyPdf(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        byte[] data = pdfExportService.generateFamilyEvolutivePdf(familyId);
        String filename = "IFE_Reporte_Evolutivo_Familia_" + familyId + "_" + System.currentTimeMillis() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/export/pdf/family/{familyId}/trajectories")
    public ResponseEntity<byte[]> exportTrajectoryPdf(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        byte[] data = pdfExportService.generateTrajectoryReportPdf(familyId);
        String filename = "IFE_Trayectorias_Familia_" + familyId + "_" + System.currentTimeMillis() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    /**
     * Revision operativa semanal: familias activas (plan ACCEPTED), misiones,
     * evidencias recientes, adherencia por periodo y comparacion contra el
     * periodo anterior. Sin parametros, cubre los ultimos 7 dias (hoy incluido).
     */
    @GetMapping("/export/operational-review")
    public ResponseEntity<byte[]> exportOperationalReview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) throws IOException {
        LocalDate end = periodEnd != null ? periodEnd : LocalDate.now();
        LocalDate start = periodStart != null ? periodStart : end.minusDays(6);

        byte[] data = excelExportService.generateOperationalReviewExcel(start, end);
        String filename = "integrity-family-operational-review_"
                + start.format(DateTimeFormatter.ISO_DATE) + "_" + end.format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}


