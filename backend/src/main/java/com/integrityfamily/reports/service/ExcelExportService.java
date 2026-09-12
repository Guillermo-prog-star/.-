package com.integrityfamily.reports.service;

import com.integrityfamily.reports.service.OperationalReviewService.EvidenceRow;
import com.integrityfamily.reports.service.OperationalReviewService.FamilyReviewRow;
import com.integrityfamily.reports.service.OperationalReviewService.MissionRow;
import com.integrityfamily.reports.service.OperationalReviewService.OperationalReview;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportService reportService;
    private final OperationalReviewService operationalReviewService;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Revision operativa semanal (familias activas + misiones + evidencias + adherencia
     * por periodo + comparacion vs periodo anterior). Ver OperationalReviewService para
     * la definicion exacta de cada metrica.
     */
    public byte[] generateOperationalReviewExcel(LocalDate periodStart, LocalDate periodEnd) throws IOException {
        OperationalReview review = operationalReviewService.generate(periodStart, periodEnd);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createResumenSheet(workbook, review);
            createMisionesSheet(workbook, review.getMissions());
            createEvidenciasSheet(workbook, review.getEvidences());
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createResumenSheet(Workbook workbook, OperationalReview review) {
        Sheet sheet = workbook.createSheet("01_RESUMEN");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle warnStyle = workbook.createCellStyle();
        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);
        warnStyle.setFont(redFont);

        int rowIdx = 0;
        Row info = sheet.createRow(rowIdx++);
        info.createCell(0).setCellValue("Periodo actual: " + review.getPeriodStart() + " a " + review.getPeriodEnd()
                + "  |  Periodo anterior: " + review.getPreviousPeriodStart() + " a " + review.getPreviousPeriodEnd());
        rowIdx++; // espacio

        String[] columns = {
                "family_id", "family_code", "family_name",
                "missions_planned", "missions_completed", "missions_overdue",
                "evidence_count", "last_evidence_at",
                "adherence_current", "adherence_previous", "adherence_delta_pp",
                "flags"
        };
        Row headerRow = sheet.createRow(rowIdx++);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (FamilyReviewRow r : review.getFamilies()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getFamilyId());
            row.createCell(1).setCellValue(nullToEmpty(r.getFamilyCode()));
            row.createCell(2).setCellValue(nullToEmpty(r.getFamilyName()));
            row.createCell(3).setCellValue(r.getMissionsPlanned());
            row.createCell(4).setCellValue(r.getMissionsCompleted());
            row.createCell(5).setCellValue(r.getMissionsOverdue());
            row.createCell(6).setCellValue(r.getEvidenceCount());
            row.createCell(7).setCellValue(r.getLastEvidenceAt() != null ? r.getLastEvidenceAt().format(DATETIME_FMT) : "");
            row.createCell(8).setCellValue(r.getAdherenceCurrent() != null ? r.getAdherenceCurrent() : Double.NaN);
            row.createCell(9).setCellValue(r.getAdherencePrevious() != null ? r.getAdherencePrevious() : Double.NaN);
            row.createCell(10).setCellValue(r.getAdherenceDeltaPp() != null ? r.getAdherenceDeltaPp() : Double.NaN);
            String flags = String.join(", ", r.getFlags());
            row.createCell(11).setCellValue(flags);
            if (!r.getFlags().isEmpty()) {
                row.getCell(11).setCellStyle(warnStyle);
            }
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createMisionesSheet(Workbook workbook, List<MissionRow> missions) {
        Sheet sheet = workbook.createSheet("02_MISIONES");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] columns = {"task_id", "family_id", "family_code", "title", "dimension", "due_date", "completed", "overdue"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (MissionRow m : missions) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(m.getTaskId());
            row.createCell(1).setCellValue(m.getFamilyId());
            row.createCell(2).setCellValue(nullToEmpty(m.getFamilyCode()));
            row.createCell(3).setCellValue(nullToEmpty(m.getTitle()));
            row.createCell(4).setCellValue(nullToEmpty(m.getDimension()));
            row.createCell(5).setCellValue(m.getDueDate() != null ? m.getDueDate().format(DATETIME_FMT) : "");
            row.createCell(6).setCellValue(m.isCompleted());
            row.createCell(7).setCellValue(m.isOverdue());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createEvidenciasSheet(Workbook workbook, List<EvidenceRow> evidences) {
        Sheet sheet = workbook.createSheet("03_EVIDENCIAS");
        CellStyle headerStyle = createHeaderStyle(workbook);

        String[] columns = {"evidence_id", "family_id", "family_code", "evidence_type", "status", "created_at", "submitted_by"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (EvidenceRow e : evidences) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(e.getEvidenceId());
            row.createCell(1).setCellValue(e.getFamilyId());
            row.createCell(2).setCellValue(nullToEmpty(e.getFamilyCode()));
            row.createCell(3).setCellValue(nullToEmpty(e.getEvidenceType()));
            row.createCell(4).setCellValue(nullToEmpty(e.getStatus()));
            row.createCell(5).setCellValue(e.getCreatedAt() != null ? e.getCreatedAt().format(DATETIME_FMT) : "");
            row.createCell(6).setCellValue(nullToEmpty(e.getSubmittedBy()));
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public byte[] generateConsolidatedExcel() throws IOException {
        ReportService.ConsolidatedReport report = reportService.generateConsolidatedReport();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 1. Pestaña: Resumen Ejecutivo
            createExecutiveSummarySheet(workbook, report);

            // 2. Pestaña: Casos Críticos (Semaforización)
            createCriticalCasesSheet(workbook, report.getCasosAltoRiesgo());

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createExecutiveSummarySheet(Workbook workbook, ReportService.ConsolidatedReport report) {
        Sheet sheet = workbook.createSheet("Resumen Ejecutivo");
        
        // Estilos
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        int rowIdx = 0;
        Row headerRow = sheet.createRow(rowIdx++);
        headerRow.createCell(0).setCellValue("Métrica Institucional");
        headerRow.createCell(1).setCellValue("Valor / Estado");
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        Row row1 = sheet.createRow(rowIdx++);
        row1.createCell(0).setCellValue("Total Familias en Fase:");
        row1.createCell(1).setCellValue(report.getMetadata().get("total_familias").toString());

        Row row2 = sheet.createRow(rowIdx++);
        row2.createCell(0).setCellValue("ID del Reporte:");
        row2.createCell(1).setCellValue(report.getReportId());

        sheet.createRow(rowIdx++); // Espacio

        // Dimensiones
        Row dimHeader = sheet.createRow(rowIdx++);
        dimHeader.createCell(0).setCellValue("Dimensión Pedagógica");
        dimHeader.createCell(1).setCellValue("Score Promedio (%)");
        dimHeader.createCell(2).setCellValue("Nivel de Alerta");
        dimHeader.getCell(0).setCellStyle(headerStyle);
        dimHeader.getCell(1).setCellStyle(headerStyle);
        dimHeader.getCell(2).setCellStyle(headerStyle);

        for (Map.Entry<String, ReportService.DimensionSummary> entry : report.getConsolidadoDimensiones().entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey().toUpperCase());
            row.createCell(1).setCellValue(entry.getValue().getPromedioScore());
            row.createCell(2).setCellValue(entry.getValue().getNivelAlerta());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createCriticalCasesSheet(Workbook workbook, List<ReportService.CaseRegistry> cases) {
        Sheet sheet = workbook.createSheet("Casos Críticos");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        CellStyle criticalStyle = workbook.createCellStyle();
        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);
        criticalStyle.setFont(redFont);

        int rowIdx = 0;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] columns = {"ID Familia", "Score Actual (%)", "Dimensión Crítica", "Impacto (Delta)"};
        
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        for (ReportService.CaseRegistry reg : cases) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(reg.getFamiliaId());
            row.createCell(1).setCellValue(reg.getPuntuacionTotal());
            row.createCell(2).setCellValue(reg.getDimensionCritica());
            row.createCell(3).setCellValue(reg.getImpactoDelta());
            
            // Resaltar en rojo si el score es bajo
            if (reg.getPuntuacionTotal() < 50) {
                row.getCell(1).setCellStyle(criticalStyle);
            }
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}


