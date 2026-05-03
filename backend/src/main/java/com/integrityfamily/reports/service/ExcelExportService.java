package com.integrityfamily.reports.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportService reportService;

    public byte[] generateConsolidatedExcel() throws IOException {
        ReportService.ConsolidatedReport report = reportService.generateConsolidatedReport();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 1. PestaÃƒÂ±a: Resumen Ejecutivo
            createExecutiveSummarySheet(workbook, report);

            // 2. PestaÃƒÂ±a: Casos CrÃƒÂ­ticos (SemaforizaciÃƒÂ³n)
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
        headerRow.createCell(0).setCellValue("MÃƒÂ©trica Institucional");
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
        dimHeader.createCell(0).setCellValue("DimensiÃƒÂ³n PedagÃƒÂ³gica");
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
        Sheet sheet = workbook.createSheet("Casos CrÃƒÂ­ticos");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        CellStyle criticalStyle = workbook.createCellStyle();
        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);
        criticalStyle.setFont(redFont);

        int rowIdx = 0;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] columns = {"ID Familia", "Score Actual (%)", "DimensiÃƒÂ³n CrÃƒÂ­tica", "Impacto (Delta)"};
        
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


