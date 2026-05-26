package com.integrityfamily.reports.service;

import com.integrityfamily.analytics.service.SentimentAnalyticsService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.integrityfamily.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PdfExportService: Generador de Dashboards Visuales Premium.
 * REDISEÑO EJECUTIVO: Estética corporativa y clínica para juntas directivas e instituciones.
 * Extendido para soportar Reportes Evolutivos Individuales de Familias del Modelo Adaptativo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final ReportService reportService;
    private final SentimentAnalyticsService sentimentAnalyticsService;
    private final AdminAlertRepository adminAlertRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final InferenceRecordRepository inferenceRecordRepository;
    private final FamilyAlertRepository familyAlertRepository;

    // Paleta de Colores Corporativos y Clínicos
    private static final Color PRIMARY_BLUE = new DeviceRgb(28, 40, 65);
    private static final Color ACCENT_BLUE = new DeviceRgb(51, 122, 183);
    private static final Color ACCENT_INDIGO = new DeviceRgb(99, 102, 241);
    private static final Color RISK_RED = new DeviceRgb(200, 35, 51);
    private static final Color SUCCESS_GREEN = new DeviceRgb(40, 167, 69);
    private static final Color LIGHT_GRAY = new DeviceRgb(245, 245, 245);
    private static final Color BORDER_GRAY = new DeviceRgb(220, 224, 230);

    /**
     * Reporte Consolidado Global (Estilo Tablero Ejecutivo)
     */
    public byte[] generateConsolidatedPdf() {
        log.info("📊 [PDF-EXPORT] Generando Reporte de Impacto Consolidado...");
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
            headerTable.addCell(new Cell().add(new Paragraph("REPORTE DE IMPACTO ESTRATÉGICO")
                    .setFontSize(18).setBold().setFontColor(PRIMARY_BLUE))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            
            headerTable.addCell(new Cell().add(new Paragraph("INTEGRITY\nFAMILY")
                    .setFontSize(14).setBold().setFontColor(ACCENT_BLUE).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            document.add(headerTable.setMarginBottom(10));
            
            document.add(new Paragraph("Rendición de Cuentas Institucional · Ecosistema de Bienestar Digital · Fase Alfa")
                    .setFontSize(9).setItalic().setFontColor(ColorConstants.GRAY).setMarginBottom(25));

            // 2. SUMMARY CARDS (Macro-Métricas)
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).setWidth(UnitValue.createPercentValue(100));
            summaryTable.addCell(createSummaryCard("FAMILIAS", String.valueOf(report.getMetadata().get("total_familias")), "Nodos Activos"));
            summaryTable.addCell(createSummaryCard("SCORE GLOBAL", "74%", "Índice de Bienestar")); 
            summaryTable.addCell(createSummaryCard("DISRUPCIONES", String.valueOf(alerts.size()), "Protocolos Sentinel"));
            document.add(summaryTable.setMarginBottom(30));

            // 3. BALANCE PEDAGÓGICO (Tabla Estilizada)
            document.add(new Paragraph("I. ESTADO DE LAS DIMENSIONES PEDAGÓGICAS").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table dimTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30})).setWidth(UnitValue.createPercentValue(100));
            dimTable.addHeaderCell(new Cell().add(new Paragraph("Dimensión Académica")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            dimTable.addHeaderCell(new Cell().add(new Paragraph("Score Promedio")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            dimTable.addHeaderCell(new Cell().add(new Paragraph("Nivel de Alerta")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());

            for (Map.Entry<String, ReportService.DimensionSummary> entry : report.getConsolidadoDimensiones().entrySet()) {
                dimTable.addCell(entry.getKey().toUpperCase());
                dimTable.addCell((int) entry.getValue().getPromedioScore() + "%");
                String alertTxt = entry.getValue().getNivelAlerta();
                Cell alertCell = new Cell().add(new Paragraph(alertTxt));
                if ("Alto".equalsIgnoreCase(alertTxt)) alertCell.setFontColor(RISK_RED).setBold();
                else if ("Controlado".equalsIgnoreCase(alertTxt)) alertCell.setFontColor(SUCCESS_GREEN);
                dimTable.addCell(alertCell);
            }
            document.add(dimTable.setMarginBottom(30));

            // 4. VOZ DEL USUARIO (IA Summary en bloque destacado)
            document.add(new Paragraph("II. ANÁLISIS DE SENTIMIENTO GLOBAL (CLAUDE AI)").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table aiBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
            aiBox.addCell(new Cell().add(new Paragraph(sentiment.getAiExecutiveSummary())
                    .setFontSize(9).setItalic().setFontColor(PRIMARY_BLUE).setPadding(10))
                    .setBackgroundColor(LIGHT_GRAY).setBorder(Border.NO_BORDER));
            document.add(aiBox.setMarginBottom(30));

            // 5. SEGURIDAD & PROTOCOLO WATCHDOG
            document.add(new Paragraph("III. MONITOR DE SEGURIDAD 'WATCHDOG'").setBold().setFontSize(12).setFontColor(RISK_RED).setMarginBottom(10));
            Table alertTable = new Table(UnitValue.createPercentArray(new float[]{25, 55, 20})).setWidth(UnitValue.createPercentValue(100));
            alertTable.addHeaderCell(new Cell().add(new Paragraph("Evento")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold());
            alertTable.addHeaderCell(new Cell().add(new Paragraph("Descripción")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold());
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
            document.add(new Paragraph("IV. IDENTIFICACIÓN DE NODOS CRÍTICOS").setBold().setFontSize(12).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table riskTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 30, 20})).setWidth(UnitValue.createPercentValue(100));
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Cód. Familia")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Score Actual")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Punto de Tensión")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());
            riskTable.addHeaderCell(new Cell().add(new Paragraph("Evolución")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold());

            for (ReportService.CaseRegistry reg : report.getCasosAltoRiesgo()) {
                riskTable.addCell(reg.getFamiliaId());
                riskTable.addCell(new Cell().add(new Paragraph((int) reg.getPuntuacionTotal() + "%").setFontColor(RISK_RED)));
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
            throw new RuntimeException("Falla en la generación del PDF Ejecutivo: " + e.getMessage(), e);
        }
    }

    /**
     * Reporte Clínico Evolutivo Individual por Familia (v5.0 Premium)
     * Inyecta dimensiones críticas, progresión adaptativa psicométrica y telemetría de la terminal CLI.
     */
    public byte[] generateFamilyEvolutivePdf(Long familyId) {
        log.info("🏡 [PDF-EXPORT] Generando Reporte Evolutivo Individual para Familia ID: {}", familyId);
        
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada para ID: " + familyId, "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));
        
        // Cargar última evaluación finalizada (para diagnosticar ICF y vulnerabilidades)
        Optional<Evaluation> lastEvalOpt = evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED);
        
        // Obtener correos de los usuarios pertenecientes a la familia para rastrear telemetría CLI
        List<User> familyUsers = userRepository.findByFamilyId(familyId);
        List<String> userEmails = familyUsers.stream().map(User::getEmail).collect(Collectors.toList());
        
        // Rastrear eventos de telemetría de ejecución de comandos CLI en esta familia
        List<AuditEvent> cliEvents = new ArrayList<>();
        if (!userEmails.isEmpty()) {
            cliEvents = auditEventRepository.findByActorEmailInOrderByOccurredAtDesc(userEmails).stream()
                    .filter(e -> "CLI_COMMAND_EXECUTED".equals(e.getEventType().name()))
                    .limit(10) // Mostrar últimos 10 comandos por limpieza del reporte
                    .collect(Collectors.toList());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // 1. HEADER CLÍNICO
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).setWidth(UnitValue.createPercentValue(100));
            headerTable.addCell(new Cell().add(new Paragraph("REPORTE EVOLUTIVO DE TRANSFORMACIÓN FAMILIAR")
                    .setFontSize(15).setBold().setFontColor(PRIMARY_BLUE))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            
            headerTable.addCell(new Cell().add(new Paragraph("INTEGRITY\nFAMILY")
                    .setFontSize(14).setBold().setFontColor(ACCENT_INDIGO).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            document.add(headerTable.setMarginBottom(8));
            
            document.add(new Paragraph("Motor de Diagnóstico Adaptativo Híbrido · Análisis Psicométrico Longitudinal · Nodo Central")
                    .setFontSize(8).setItalic().setFontColor(ColorConstants.GRAY).setMarginBottom(20));

            // 2. FAMILY INFO CARD (Ficha de Caracterización)
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).setWidth(UnitValue.createPercentValue(100));
            
            Cell leftInfo = new Cell().add(new Paragraph("NÚCLEO FAMILIAR: ").setBold().setFontColor(PRIMARY_BLUE).setFontSize(9))
                    .add(new Paragraph(family.getName()).setFontSize(12).setBold().setFontColor(ACCENT_BLUE))
                    .add(new Paragraph("Código Familiar: " + family.getFamilyCode() + "  |  Ubicación: " + (family.getMunicipio() != null ? family.getMunicipio() : "No especificada")).setFontSize(8).setFontColor(ColorConstants.GRAY))
                    .setBorder(new SolidBorder(BORDER_GRAY, 1)).setPadding(10).setBackgroundColor(LIGHT_GRAY);
            
            String currentMilestone = family.getCurrentMilestone() != null ? family.getCurrentMilestone() : "W1";
            Cell rightInfo = new Cell().add(new Paragraph("HITO DE MADUREZ ACTUAL: ").setBold().setFontColor(PRIMARY_BLUE).setFontSize(9))
                    .add(new Paragraph(getMilestoneLabel(currentMilestone)).setFontSize(12).setBold().setFontColor(SUCCESS_GREEN))
                    .add(new Paragraph("Monitoreo Sentinel: " + (Boolean.TRUE.equals(family.getSentinelActive()) ? "ACTIVO 🛡️" : "INACTIVO") + "  |  Evaluaciones Realizadas: " + evaluationRepository.findByFamilyId(familyId).size()).setFontSize(8).setFontColor(ColorConstants.GRAY))
                    .setBorder(new SolidBorder(BORDER_GRAY, 1)).setPadding(10).setBackgroundColor(LIGHT_GRAY);
            
            infoTable.addCell(leftInfo);
            infoTable.addCell(rightInfo);
            document.add(infoTable.setMarginBottom(25));

            // 3. SECCIÓN I: ÍNDICE DE CONECTIVIDAD FAMILIAR (ICF)
            document.add(new Paragraph("I. ÍNDICE DE CONECTIVIDAD FAMILIAR (ICF)").setBold().setFontSize(11).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            
            if (lastEvalOpt.isEmpty()) {
                Table noEvalBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
                noEvalBox.addCell(new Cell().add(new Paragraph("CRITERIO CLÍNICO: SIN EVALUACIÓN FINALIZADA COMPLETA\n\nEste núcleo familiar se encuentra en fase de inducción inicial. Aún no se registra un diagnóstico finalizado en la base de datos MySQL de Spring Boot. Por favor complete la evaluación diagnóstica adaptativa desde el panel web para calcular el ICF de partida.")
                        .setFontSize(9).setFontColor(RISK_RED).setPadding(12))
                        .setBackgroundColor(new DeviceRgb(254, 242, 242)).setBorder(new SolidBorder(RISK_RED, 1)));
                document.add(noEvalBox.setMarginBottom(25));
            } else {
                Evaluation lastEval = lastEvalOpt.get();
                double icfValue = lastEval.getIcf() != null ? lastEval.getIcf() : 0.0;
                String finalizedStr = lastEval.getFinalizedAt() != null ? lastEval.getFinalizedAt().format(df) : "N/A";

                Table icfTable = new Table(UnitValue.createPercentArray(new float[]{30, 70})).setWidth(UnitValue.createPercentValue(100));
                
                Cell icfMetricCell = new Cell().add(new Paragraph("ICF ACTUAL").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY))
                        .add(new Paragraph(String.format("%.0f%%", icfValue)).setFontSize(28).setBold().setFontColor(getIcfColor(icfValue)))
                        .add(new Paragraph("Sincronizado: " + finalizedStr).setFontSize(6).setFontColor(ColorConstants.GRAY))
                        .setBorder(new SolidBorder(BORDER_GRAY, 1)).setBackgroundColor(LIGHT_GRAY).setPadding(10).setTextAlignment(TextAlignment.CENTER);
                
                Cell icfDescCell = new Cell().add(new Paragraph("DIAGNÓSTICO Y PERSPECTIVA CONDUCTUAL").setFontSize(9).setBold().setFontColor(PRIMARY_BLUE))
                        .add(new Paragraph(getIcfClinicalDescription(icfValue)).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY))
                        .add(new Paragraph("\nSostenibilidad: " + (lastEval.getHasCrisis() != null && lastEval.getHasCrisis() ? "⚠️ CRITICAL MODE (Se activó el protocolo de contención / alerta conductual)." : "✓ Estabilidad estructural controlada."))
                                .setFontSize(8).setBold().setFontColor(lastEval.getHasCrisis() != null && lastEval.getHasCrisis() ? RISK_RED : SUCCESS_GREEN))
                        .setBorder(new SolidBorder(BORDER_GRAY, 1)).setPadding(10).setBackgroundColor(LIGHT_GRAY);
                
                icfTable.addCell(icfMetricCell);
                icfTable.addCell(icfDescCell);
                document.add(icfTable.setMarginBottom(25));

                // 4. SECCIÓN II: DIAGNÓSTICO MULTIDIMENSIONAL - EQUILIBRIO DE PILARES
                document.add(new Paragraph("II. BALANCE DIAGNÓSTICO ADAPTATIVO POR DIMENSIÓN").setBold().setFontSize(11).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
                
                Table dimScoresTable = new Table(UnitValue.createPercentArray(new float[]{40, 20, 40})).setWidth(UnitValue.createPercentValue(100));
                dimScoresTable.addHeaderCell(new Cell().add(new Paragraph("Dimensión Psicométrica")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(9));
                dimScoresTable.addHeaderCell(new Cell().add(new Paragraph("Score")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(9));
                dimScoresTable.addHeaderCell(new Cell().add(new Paragraph("Priorización en Motor Adaptativo (Próximo Test)")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(9));

                List<EvaluationDimensionScore> sortedScores = lastEval.getDimensionScores().stream()
                        .sorted(Comparator.comparingDouble(EvaluationDimensionScore::getScore))
                        .collect(Collectors.toList());

                String criticalDim = sortedScores.isEmpty() ? "comunicacion" : sortedScores.get(0).getDimensionName();

                // Asegurar que las 4 dimensiones fundamentales se listen
                Map<String, Double> scoreMap = lastEval.getDimensionScores().stream()
                        .collect(Collectors.toMap(EvaluationDimensionScore::getDimensionName, EvaluationDimensionScore::getScore, (a, b) -> a));
                
                String[] coreDims = {"comunicacion", "emociones", "habitos", "tiempos"};
                for (String d : coreDims) {
                    double score = scoreMap.getOrDefault(d, 0.0);
                    dimScoresTable.addCell(new Cell().add(new Paragraph(getDimensionFriendlyName(d)).setFontSize(8).setBold()));
                    dimScoresTable.addCell(new Cell().add(new Paragraph(String.format("%.0f%%", score)).setFontSize(8).setBold().setFontColor(getIcfColor(score))));
                    
                    Cell adaptiveCell = new Cell();
                    if (d.equalsIgnoreCase(criticalDim)) {
                        adaptiveCell.add(new Paragraph("VULNERABILIDAD MÁXIMA ⚡\n(Filtro de reactivos de riesgo activo)").setFontSize(7).setFontColor(RISK_RED).setBold());
                        adaptiveCell.setBackgroundColor(new DeviceRgb(254, 242, 242));
                    } else {
                        adaptiveCell.add(new Paragraph("Monitoreo de Sostenibilidad").setFontSize(7).setFontColor(ColorConstants.GRAY));
                    }
                    dimScoresTable.addCell(adaptiveCell);
                }
                document.add(dimScoresTable.setMarginBottom(25));
            }

            // 5. SECCIÓN III: RUTA DE TRANSFORMACIÓN Y PROGRESIÓN ADAPTATIVA
            document.add(new Paragraph("III. MODELO HÍBRIDO DE PROGRESIÓN ADAPTATIVA LONGITUDINAL").setBold().setFontSize(11).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            Table modelDescBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
            Cell descCell = new Cell().add(new Paragraph("El sistema de diagnóstico de Integrity Family utiliza un algoritmo de selección híbrido adaptativo que personaliza el set de preguntas en base a dos dimensiones clínicas:\n" +
                    "1. Hito Temporal (Maturity Match): Servimos reactivos de acuerdo al mes de maduración actual del hogar (Hitos W1 a M36).\n" +
                    "2. Profundización Asincrónica: Si se detecta un descenso en un pilar, el motor recluta automáticamente 6 preguntas adaptativas de esa dimensión específica en el siguiente ciclo diagnóstico, evitando respuestas memorizadas y midiendo evolución conductual sostenible.")
                    .setFontSize(8).setFontColor(ColorConstants.DARK_GRAY))
                    .setBackgroundColor(LIGHT_GRAY).setBorder(new SolidBorder(BORDER_GRAY, 1)).setPadding(10);
            modelDescBox.addCell(descCell);
            document.add(modelDescBox.setMarginBottom(25));

            // 6. SECCIÓN IV: DIAGNÓSTICO SCANNER IF-TOS / IF-CIS
            document.add(new Paragraph("IV. ESTADO OPERACIONAL DEL SCANNER (IF-TOS / IF-CIS)").setBold().setFontSize(11).setFontColor(PRIMARY_BLUE).setMarginBottom(10));

            List<InferenceRecord> inferenceRecords = inferenceRecordRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
            List<FamilyAlert> activeAlerts = familyAlertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(familyId);

            if (inferenceRecords.isEmpty()) {
                Table noScanBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
                noScanBox.addCell(new Cell().add(new Paragraph("El motor scanner IF-TOS aún no ha procesado ninguna evaluación para este núcleo familiar.")
                        .setFontSize(8).setItalic().setFontColor(ColorConstants.GRAY).setPadding(10))
                        .setBackgroundColor(LIGHT_GRAY).setBorder(new SolidBorder(BORDER_GRAY, 1)));
                document.add(noScanBox.setMarginBottom(20));
            } else {
                InferenceRecord latest = inferenceRecords.get(0);
                String tosState = latest.getOperationalState() != null ? latest.getOperationalState() : "DESCONOCIDO";
                double uncertainty = latest.getUncertaintyTotal() != null ? latest.getUncertaintyTotal() : 0.0;
                boolean simSuspected = Boolean.TRUE.equals(latest.getSimulationSuspected());

                Table tosTable = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34})).setWidth(UnitValue.createPercentValue(100));

                Cell tosCell = new Cell().add(new Paragraph("ESTADO IF-TOS").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY))
                        .add(new Paragraph(tosState).setFontSize(14).setBold().setFontColor(getTosColor(tosState)))
                        .setBorder(new SolidBorder(BORDER_GRAY, 1)).setBackgroundColor(LIGHT_GRAY).setPadding(10).setTextAlignment(TextAlignment.CENTER);

                Cell uncCell = new Cell().add(new Paragraph("IF-SUM INCERTIDUMBRE").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY))
                        .add(new Paragraph(String.format("%.0f%%", uncertainty * 100)).setFontSize(14).setBold().setFontColor(uncertainty > 0.30 ? RISK_RED : SUCCESS_GREEN))
                        .add(new Paragraph(uncertainty > 0.30 ? "Alta variabilidad" : "Señal estable").setFontSize(7).setFontColor(ColorConstants.GRAY))
                        .setBorder(new SolidBorder(BORDER_GRAY, 1)).setBackgroundColor(LIGHT_GRAY).setPadding(10).setTextAlignment(TextAlignment.CENTER);

                Cell simCell = new Cell().add(new Paragraph("SIMULACIÓN DETECTADA").setFontSize(8).setBold().setFontColor(ColorConstants.GRAY))
                        .add(new Paragraph(simSuspected ? "SOSPECHA ACTIVA" : "Sin indicios").setFontSize(14).setBold().setFontColor(simSuspected ? RISK_RED : SUCCESS_GREEN))
                        .setBorder(new SolidBorder(BORDER_GRAY, 1)).setBackgroundColor(LIGHT_GRAY).setPadding(10).setTextAlignment(TextAlignment.CENTER);

                tosTable.addCell(tosCell);
                tosTable.addCell(uncCell);
                tosTable.addCell(simCell);
                document.add(tosTable.setMarginBottom(15));

                // Últimos registros de inferencia IF-CIS
                document.add(new Paragraph("Historial IF-CIS (últimas inferencias)").setFontSize(9).setBold().setFontColor(PRIMARY_BLUE).setMarginBottom(6));
                Table cisTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20})).setWidth(UnitValue.createPercentValue(100));
                cisTable.addHeaderCell(new Cell().add(new Paragraph("Clave")).setBackgroundColor(ACCENT_INDIGO).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                cisTable.addHeaderCell(new Cell().add(new Paragraph("ICF")).setBackgroundColor(ACCENT_INDIGO).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                cisTable.addHeaderCell(new Cell().add(new Paragraph("Riesgo")).setBackgroundColor(ACCENT_INDIGO).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                cisTable.addHeaderCell(new Cell().add(new Paragraph("Estado")).setBackgroundColor(ACCENT_INDIGO).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                cisTable.addHeaderCell(new Cell().add(new Paragraph("Ep. State")).setBackgroundColor(ACCENT_INDIGO).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                for (InferenceRecord r : inferenceRecords.stream().limit(5).collect(Collectors.toList())) {
                    cisTable.addCell(new Cell().add(new Paragraph(r.getInferenceKey() != null ? r.getInferenceKey() : "—").setFontSize(7)));
                    cisTable.addCell(new Cell().add(new Paragraph(r.getIcfValue() != null ? String.format("%.0f%%", r.getIcfValue()) : "—").setFontSize(7)));
                    cisTable.addCell(new Cell().add(new Paragraph(r.getRiskLevel() != null ? r.getRiskLevel() : "—").setFontSize(7)));
                    cisTable.addCell(new Cell().add(new Paragraph(r.getOperationalState() != null ? r.getOperationalState() : "—").setFontSize(7)));
                    cisTable.addCell(new Cell().add(new Paragraph(r.getEpistemicState() != null ? r.getEpistemicState() : "—").setFontSize(7)));
                }
                document.add(cisTable.setMarginBottom(15));

                // Alertas activas IF-ALT
                if (!activeAlerts.isEmpty()) {
                    document.add(new Paragraph("Alertas IF-ALT activas no resueltas: " + activeAlerts.size()).setFontSize(9).setBold().setFontColor(RISK_RED).setMarginBottom(6));
                    Table altTable = new Table(UnitValue.createPercentArray(new float[]{30, 50, 20})).setWidth(UnitValue.createPercentValue(100));
                    altTable.addHeaderCell(new Cell().add(new Paragraph("Tipo")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                    altTable.addHeaderCell(new Cell().add(new Paragraph("Título / Detalle")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                    altTable.addHeaderCell(new Cell().add(new Paragraph("Severidad")).setBackgroundColor(RISK_RED).setFontColor(ColorConstants.WHITE).setBold().setFontSize(7));
                    for (FamilyAlert alert : activeAlerts.stream().limit(5).collect(Collectors.toList())) {
                        altTable.addCell(new Cell().add(new Paragraph(alert.getAlertType() != null ? alert.getAlertType() : "—").setFontSize(7)));
                        String alertText = (alert.getTitle() != null ? alert.getTitle() : "")
                                + (alert.getDetail() != null ? "\n" + alert.getDetail() : "");
                        altTable.addCell(new Cell().add(new Paragraph(alertText.isBlank() ? "—" : alertText.trim()).setFontSize(7)));
                        altTable.addCell(new Cell().add(new Paragraph(alert.getSeverity() != null ? alert.getSeverity() : "—").setFontSize(7).setBold()
                                .setFontColor("HIGH".equalsIgnoreCase(alert.getSeverity()) || "CRITICAL".equalsIgnoreCase(alert.getSeverity()) ? RISK_RED : ColorConstants.DARK_GRAY)));
                    }
                    document.add(altTable.setMarginBottom(20));
                }
            }

            // 7. SECCIÓN V: AUDITORÍA DE TELEMETRÍA DE LA CONSOLA CLI
            document.add(new Paragraph("V. REGISTRO DE TELEMETRÍA DE LA TERMINAL CLI DE LA FAMILIA").setBold().setFontSize(11).setFontColor(PRIMARY_BLUE).setMarginBottom(10));
            
            if (cliEvents.isEmpty()) {
                Table noCliBox = new Table(1).setWidth(UnitValue.createPercentValue(100));
                noCliBox.addCell(new Cell().add(new Paragraph("No se registran eventos de telemetría de la Consola CLI para este núcleo familiar en las últimas fases.\nSugerencia: Promueva la interacción de los miembros de la familia con la terminal del plan para auditar telemetría conductual.")
                        .setFontSize(8).setItalic().setFontColor(ColorConstants.GRAY).setPadding(10))
                        .setBackgroundColor(LIGHT_GRAY).setBorder(new SolidBorder(BORDER_GRAY, 1)));
                document.add(noCliBox.setMarginBottom(20));
            } else {
                Table cliTable = new Table(UnitValue.createPercentArray(new float[]{25, 20, 55})).setWidth(UnitValue.createPercentValue(100));
                cliTable.addHeaderCell(new Cell().add(new Paragraph("Fecha y Hora")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(8));
                cliTable.addHeaderCell(new Cell().add(new Paragraph("Usuario (Email)")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(8));
                cliTable.addHeaderCell(new Cell().add(new Paragraph("Comando CLI Ejecutado")).setBackgroundColor(PRIMARY_BLUE).setFontColor(ColorConstants.WHITE).setBold().setFontSize(8));

                for (AuditEvent event : cliEvents) {
                    cliTable.addCell(new Cell().add(new Paragraph(event.getOccurredAt().format(df)).setFontSize(7)));
                    cliTable.addCell(new Cell().add(new Paragraph(event.getActorEmail()).setFontSize(7)));
                    cliTable.addCell(new Cell().add(new Paragraph(extractCommandFromMetadata(event.getMetadataJson()))
                            .setFontSize(7).setBold().setFontColor(ACCENT_BLUE)));
                }
                document.add(cliTable.setMarginBottom(20));
            }

            // FOOTER CLINICO
            document.add(new Paragraph("\n\n-- Este reporte representa un documento clínico y de auditoría confidencial emitido por la Plataforma Integrity Family --")
                    .setFontSize(7).setTextAlignment(TextAlignment.CENTER).setFontColor(ColorConstants.GRAY));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("❌ [PDF-EXPORT] Error generando PDF Evolutivo para la familia: {}", familyId, e);
            throw new RuntimeException("Falla en la generación del PDF Evolutivo Familiar: " + e.getMessage(), e);
        }
    }

    /**
     * Helpers de Estilo y Visualización de Reportes
     */
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

    private String getMilestoneLabel(String milestoneCode) {
        if (milestoneCode == null) return "Estabilización (W1)";
        switch (milestoneCode.toUpperCase()) {
            case "W1": return "Estabilización Inicial (W1)";
            case "M1": return "Conciencia Inicial (M1)";
            case "M3": return "Cimentación de Vínculos (M3)";
            case "M6": return "Transformación Profunda (M6)";
            case "M9": return "Consolidación de Hábitos (M9)";
            case "M12": return "Integridad Plena (M12)";
            case "M18": return "Crecimiento Generacional (M18)";
            case "M24": return "Legado Familiar (M24)";
            case "M30": return "Trascendencia Activa (M30)";
            case "M36": return "Plenitud Total (M36)";
            default: return "Fase " + milestoneCode;
        }
    }

    private Color getIcfColor(double icf) {
        if (icf < 40.0) return RISK_RED;
        if (icf < 65.0) return new DeviceRgb(245, 158, 11); // Amber
        return SUCCESS_GREEN;
    }

    private String getIcfClinicalDescription(double icf) {
        if (icf < 40.0) {
            return "NIVEL CRÍTICO: Se registran altos niveles de reactividad conductual e inconsciencia relacional. Las dinámicas de comunicación son mayoritariamente impulsivas u omisivas. Se requiere activar protocolos de mediación y soporte continuo.";
        }
        if (icf < 65.0) {
            return "NIVEL CONSCIENTE / REACTIVO: El núcleo familiar identifica de manera grupal sus brechas y patrones disfuncionales, pero experimenta dificultades para sostener los acuerdos en momentos de alta tensión. Oportunidad idónea de cimentación.";
        }
        if (icf < 85.0) {
            return "NIVEL INTENCIONAL / ASERTIVO: Convivencia cimentada sobre el respeto y la escucha comprensiva de manera cotidiana. Las decisiones y rutinas se coordinan activamente con un alto grado de intencionalidad y presencia emocional.";
        }
        return "NIVEL PLENO / TOTAL: La autorregulación del sistema familiar fluye de manera autónoma y asertiva. Las dinámicas afectivas se despliegan en perfecta paz, amor mutuo e integración colectiva del propósito de vida familiar.";
    }

    private String getDimensionFriendlyName(String dimension) {
        if (dimension == null) return "Consciencia Familiar";
        switch (dimension.toLowerCase()) {
            case "comunicacion": return "Comunicación Asertiva";
            case "emociones": return "Regulación & Clima Emocional";
            case "habitos": return "Hábitos & Convivencia Colectiva";
            case "tiempos": return "Tiempos de Conexión Activa";
            default: return dimension.toUpperCase();
        }
    }

    private Color getTosColor(String tosState) {
        if (tosState == null) return ColorConstants.GRAY;
        switch (tosState.toUpperCase()) {
            case "CRITICAL":    return RISK_RED;
            case "ESCALATING":  return new DeviceRgb(245, 158, 11);
            case "RECOVERING":  return ACCENT_BLUE;
            case "STABLE":
            case "RESOLVED":    return SUCCESS_GREEN;
            default:            return ColorConstants.DARK_GRAY;
        }
    }

    private String extractCommandFromMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return "Comando Ejecutado";
        }
        try {
            if (metadataJson.contains("\"command\":\"")) {
                int start = metadataJson.indexOf("\"command\":\"") + 11;
                int end = metadataJson.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return "> " + metadataJson.substring(start, end);
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return metadataJson;
    }
}
