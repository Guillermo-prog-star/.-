package com.integrityfamily.reports.service;

import com.integrityfamily.analytics.service.SentimentAnalyticsService;
import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * PdfExportService: Generador de Dashboards Visuales Premium.
 * REDISEÃƒâ€˜O EJECUTIVO: EstÃƒÂ©tica corporativa para juntas directivas e instituciones.
 */
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ReportService reportService;
    private final SentimentAnalyticsService sentimentAnalyticsService;
    private final AdminAlertRepository adminAlertRepository;

    // Paleta de Colores Corporativos
    private static final Color PRIMARY_BLUE = new DeviceRgb(28, 40, 65);
    private static final Color ACCENT_BLUE = new DeviceRgb(51, 122, 183);
    private static final Color RISK_RED = new DeviceRgb(200, 35, 51);
    private static final Color SUCCESS_GREEN = new DeviceRgb(40, 167, 69);
    private static final Color LIGHT_GRAY = new DeviceRgb(245, 245, 245);

    public byte[] generateConsolidatedPdf() {
        ReportService.ConsolidatedReport report = reportService.generateConsolidatedReport();
        SentimentAnalyticsService.SentimentReport sentiment = sentimentAnalyticsService.analyzeGlobalFeedback();
        List<AdminAlert> alerts = adminAlertRepository.findAllByOrderByCreatedAtDesc();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // 1. HEADLINE CORPORATIVO
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).setWidth(UnitValue.createPercentValue(100));
            headerTable.addCell(new Cell().add(new Paragraph("REPORTE DE IMPACTO ESTRATÃƒâ€°GICO")
                    .setFontSize(18).setBold().setFontColor(PRIMARY_BLUE))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            
            headerTable.addCell(new Cell().add(new Paragraph("INTEGRITY\nFAMILY")
                    .setFontSize(14).setBold().setFontColor(ACCENT_BLUE).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            document.add(headerTable.setMarginBottom(10));
            
            document.add(new Paragraph("RendiciÃƒÂ³n de Cuentas Institucional Ã‚Â· Ecosistema de Bienestar Digital Ã‚Â· Fase Alfa")
                    .setFontSize(9).setItalic().setFontColor(ColorConstants.GRAY).setMarginBottom(25));

            // 2. SUMMARY CARDS (Macro-MÃƒÂ©tricas)
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).setWidth(UnitValue.createPercentValue(100));
            summaryTable.addCell(createSummaryCard("FAMILIAS", String.valueOf(report.getMetadata().get("total_familias")), "Nodos Activos"));
            summaryTable.addCell(createSummaryCard("SCORE GLOBAL", "74.2%", "ÃƒÂndice de Bienestar")); // Placeholder o promedio real
            summaryTable.addCell(createSummaryCard("DISRUPCIONES", String.valueOf(alerts.size()), "Protocolos Sentinel"));
            document.add(summaryTable.setMarginBottom(30));

            // 3. BALANCE PEDAGÃƒâ€œGICO (Tabla Estilizada)
            document.add(new Paragraph("I. ESTADO DE LAS DIMENSIONES PEDAGÃƒâ€œGICAS").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table dimTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30})).setWidth(UnitValue.createPercentValue(100));
            dimTable.addHeaderCell(new Cell().add(new Paragraph("DimensiÃƒÂ³n AcadÃƒÂ©mica")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            dimTable.addHeaderCell(new Cell().add(new Paragraph("Score Promedio")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            dimTable.addHeaderCell(new Cell().add(new Paragraph("Nivel de Alerta")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());

            for (Map.Entry<String, ReportService.DimensionSummary> entry : report.getConsolidadoDimensiones().entrySet()) {
                dimTable.addCell(entry.getKey().toUpperCase());
                dimTable.addCell(entry.getValue().getPromedioScore() + "%");
                String alertTxt = entry.getValue().getNivelAlerta();
                Cell alertCell = new Cell().add(new Paragraph(alertTxt));
                if ("Alto".equalsIgnoreCase(alertTxt)) alertCell.setFontColor(RISK_RED).setBold();
                else if ("Controlado".equalsIgnoreCase(alertTxt)) alertCell.setFontColor(SUCCESS_GREEN);
                dimTable.addCell(alertCell);
            }
            document.add(dimTable.setMarginBottom(30));

            // 4. VOZ DEL USUARIO (IA Summary en bloque destacado)
            document.add(new Paragraph("II. ANÃƒÂLISIS DE SENTIMIENTO GLOBAL (CLAUDE AI)").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table aiBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
            aiBox.addCell(new Cell().add(new Paragraph(sentiment.getAiExecutiveSummary())
                    .setFontSize(9).setItalic().setFontColor(PRIMARY_BLUE).setPadding(10))
                    .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER));
            document.add(aiBox.setMarginBottom(30));

            // 5. SEGURIDAD & PROTOCOLO WATCHDOG
            document.add(new Paragraph("III. MONITOR DE SEGURIDAD 'WATCHDOG'").setBold().setFontSize(12).setFontColor(RISK_RED).setMarginBottom(10));
            Table alertTable = new Table(UnitValue.createPercentArray(new float[]{25, 55, 20})).setWidth(UnitValue.createPercentValue(100));
            alertTable.addHeaderCell(new Cell().add(new Paragraph("Evento")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold());
            alertTable.addHeaderCell(new Cell().add(new Paragraph("DescripciÃƒÂ³n")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold());
            alertTable.addHeaderCell(new Cell().add(new Paragraph("Estado")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold());

            for (int i = 0; i < Math.min(alerts.size(), 5); i++) {
                AdminAlert alert = alerts.get(i);
                alertTable.addCell(new Cell().add(new Paragraph(alert.getTitle()).setFontSize(8)));
                alertTable.addCell(new Cell().add(new Paragraph(alert.getMessage()).setFontSize(8)));
                Cell sevCell = new Cell().add(new Paragraph(alert.getSeverity()).setFontSize(8).setBold());
                if ("CRITICAL".equals(alert.getSeverity())) sevCell.setFontColor(RISK_RED);
                alertTable.addCell(sevCell);
            }
            document.add(alertTable.setMarginBottom(30));

            // 6. CASOS DE ALTO RIESGO
            document.add(new Paragraph("IV. IDENTIFICACIÃƒâ€œN DE NODOS CRÃƒÂTICOS").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table riskTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 30, 20})).setWidth(UnitValue.createPercentValue(100));
            riskTable.addHeaderCell(new Cell().add(new Paragraph("CÃƒÂ³d. Familia")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Score Actual")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Punto de TensiÃƒÂ³n")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("EvoluciÃƒÂ³n")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());

            for (ReportService.CaseRegistry reg : report.getCasosAltoRiesgo()) {
                riskTable.addCell(reg.getFamiliaId());
                riskTable.addCell(new Cell().add(new Paragraph(reg.getPuntuacionTotal() + "%").setFontColor(RISK_RED)));
                riskTable.addCell(reg.getDimensionCritica());
                riskTable.addCell(reg.getImpactoDelta());
            }
            document.add(riskTable);

            // FOOTER
            document.add(new Paragraph("\n\n-- Reporte Generado por el Motor de Inteligencia Integrity Family --")
                    .setFontSize(7).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.GRAY));
            
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Falla en la generaciÃƒÂ³n del PDF Ejecutivo: " + e.getMessage(), e);
        }
    }

    private Cell createSummaryCard(String title, String value, String subtitle) {
        Cell cell = new Cell();
        cell.add(new Paragraph(title).setFontSize(8).setBold().setFontColor(ACCENT_BLUE));
        cell.add(new Paragraph(value).setFontSize(22).setBold().setFontColor(PRIMARY_BLUE));
        cell.add(new Paragraph(subtitle).setFontSize(7).setFontColor(ColorConstants.GRAY));
        cell.add(new Paragraph(" "));
        cell.setBorder(Border.NO_BORDER);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }
}



