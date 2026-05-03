package com.integrityfamily.report.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.integrityfamily.report.dto.TransformationSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;

/**
 * SDD-REP-04: PDF Generation Service.
 */
@Service
@Slf4j
public class PdfReportService {

    public byte[] generateTransformationReport(TransformationSummary summary, String aiNarrative) {
        log.info("Ã°Å¸â€œâ€ž [PDF-ENGINE] Generating PDF for family {}", summary.familyName());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title & Header
            document.add(new Paragraph("REPORTE DE TRANSFORMACIÃƒâ€œN INTEGRAL")
                    .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Familia: " + summary.familyName())
                    .setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Metrics Section
            document.add(new Paragraph("INDICADORES DE INTEGRIDAD").setBold().setFontSize(14));
            document.add(new Paragraph("ICF Actual: " + summary.currentIcf()));
            document.add(new Paragraph("Misiones Completadas: " + summary.missionsCompleted()));
            document.add(new Paragraph("\n"));

            // AI Narrative Section
            document.add(new Paragraph("ANÃƒÂLISIS ESTRATÃƒâ€°GICO (MENTOR AI)").setBold().setFontSize(14));
            String cleanNarrative = aiNarrative.replace("###", "").replace("**", "");
            document.add(new Paragraph(cleanNarrative).setFontSize(11));

            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Documento generado automÃƒÂ¡ticamente por Integrity Family Platform")
                    .setFontSize(8).setItalic().setTextAlignment(TextAlignment.RIGHT));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation error", e);
        }
    }
}


