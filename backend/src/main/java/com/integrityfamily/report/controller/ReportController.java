package com.integrityfamily.report.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.security.SecurityValidator;
import com.integrityfamily.report.dto.TransformationSummary;
import com.integrityfamily.report.service.ExecutiveReportService;
import com.integrityfamily.report.service.PdfReportService;
import com.integrityfamily.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * SDD-REP-03: Reporting API for families.
 * Exposes consolidated metrics and AI-driven synthesis for the dashboard.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ExecutiveReportService reportService;
    private final AiService aiService;
    private final PdfReportService pdfReportService;
    private final SecurityValidator securityValidator;

    @GetMapping("/api/reports/family/{familyId}/summary")
    public ApiResponse<TransformationSummary> getSummary(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        return ApiResponse.ok(reportService.generateRawSummary(familyId));
    }

    @GetMapping("/api/reports/family/{familyId}/synthesis")
    public ApiResponse<String> getSynthesis(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        return ApiResponse.ok(aiService.generateExecutiveSynthesis(familyId));
    }

    @GetMapping("/api/reports/family/{familyId}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long familyId, Principal principal) {
        log.info("[API] PDF report requested for family: {}", familyId);
        securityValidator.validateFamilyOwnership(familyId, principal);

        TransformationSummary summary = reportService.generateRawSummary(familyId);
        String synthesis = aiService.generateExecutiveSynthesis(familyId);
        byte[] pdfBytes = pdfReportService.generateTransformationReport(summary, synthesis);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Reporte_Integridad_Fam_" + familyId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}


