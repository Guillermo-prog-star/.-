package com.integrityfamily.reports.web;

import com.integrityfamily.reports.service.ReportService;
import com.integrityfamily.reports.service.ExcelExportService;
import com.integrityfamily.reports.service.PdfExportService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ExportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

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
    public ResponseEntity<byte[]> exportFamilyPdf(@PathVariable Long familyId) {
        byte[] data = pdfExportService.generateFamilyEvolutivePdf(familyId);
        String filename = "IFE_Reporte_Evolutivo_Familia_" + familyId + "_" + System.currentTimeMillis() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}


