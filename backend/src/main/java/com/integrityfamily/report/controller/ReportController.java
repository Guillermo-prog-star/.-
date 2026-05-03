package com.integrityfamily.report.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.security.SecurityValidator;
import com.integrityfamily.report.dto.TransformationSummary;
import com.integrityfamily.report.service.ExecutiveReportService;
import com.integrityfamily.report.service.PdfReportService; // SINCRONIZACIÃƒâ€œN: Import aÃƒÂ±adido
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ExecutiveReportService reportService;
    private final AiService aiService;
    private final PdfReportService pdfReportService; // SÃƒÂ­mbolo ahora reconocido
    private final SecurityValidator securityValidator;

    /**
     * Returns the raw transformation summary (metrics only).
     */
    @GetMapping("/family/{familyId}/summary")
    public ApiResponse<TransformationSummary> getSummary(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        return ApiResponse.ok(reportService.generateRawSummary(familyId));
    }

    /**
     * Returns the AI-generated executive synthesis (narrative).
     */
    @GetMapping("/family/{familyId}/synthesis")
    public ApiResponse<String> getSynthesis(@PathVariable Long familyId, Principal principal) {
        securityValidator.validateFamilyOwnership(familyId, principal);
        return ApiResponse.ok(aiService.generateExecutiveSynthesis(familyId));
    }

    /**
     * Generates and downloads the full PDF report.
     */
    @GetMapping("/family/{familyId}/download")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long familyId, Principal principal) {
        log.info("Ã°Å¸â€œÂ¥ [API] Request to download PDF for family: {}", familyId);
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


