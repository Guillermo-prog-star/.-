package com.integrityfamily.twin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.twin.domain.FamilyPrediction;
import com.integrityfamily.twin.domain.FamilyTwinProfile;
import com.integrityfamily.twin.dto.DigitalTwinDto;
import com.integrityfamily.twin.repository.FamilyPredictionRepository;
import com.integrityfamily.twin.repository.FamilyTwinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigitalTwinService {

    private final FamilyTwinRepository       twinRepository;
    private final FamilyPredictionRepository predictionRepository;
    private final FamilyRepository           familyRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;
    private final TaskEvidenceRepository     evidenceRepository;
    private final FamilyLogbookRepository    logbookRepository;
    private final CriticalDayRepository      crisisRepository;
    private final FamilySprintRepository     sprintRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final FamilyContextEngine        contextEngine;
    private final FamilyDnaService           dnaService;
    private final AiProvider                 aiProvider;
    private final ObjectMapper               objectMapper;

    // ─── Consulta pública ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<DigitalTwinDto> getTwin(Long familyId) {
        return twinRepository.findByFamilyId(familyId).map(twin -> {
            Family family = familyRepository.findById(familyId).orElseThrow();
            List<FamilyPrediction> predictions = predictionRepository
                    .findByFamilyIdAndStatusOrderByConfidenceDesc(familyId, "ACTIVE");
            return toDto(twin, family.getName(), predictions);
        });
    }

    @Transactional
    public DigitalTwinDto compute(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        log.info("[TWIN] Computando Gemelo Digital para familia {}", familyId);

        // ── Recopila todo el historial ───────────────────────────────────────
        var lts        = ltsRepository.findByFamilyId(familyId).orElse(null);
        var gratitudes = gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var evidences  = evidenceRepository.findByFamilyId(familyId);
        var logbooks   = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var crises     = crisisRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var sprints    = sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        var context    = contextEngine.buildContextBlock(familyId);
        var dna        = dnaService.buildDnaContextBlock(familyId);

        // ── Analiza patrones ─────────────────────────────────────────────────
        String dataRichness  = computeDataRichness(gratitudes, evidences, logbooks, crises);
        double resilienceIdx = computeResilienceIndex(lts, crises);
        String bondingRhythm = computeBondingRhythm(gratitudes, evidences, logbooks);
        String commPattern   = "COLLABORATIVE"; // Se refinará con FamilyIdentityProfile en versiones futuras
        String peakDay       = computePeakDay(gratitudes, evidences, logbooks);
        Integer avgBetweenCrises = computeAvgDaysBetweenCrises(crises);
        Integer avgRecovery      = computeAvgRecoveryDays(lts);

        List<DigitalTwinDto.DetectedPattern> patterns  = detectPatterns(lts, gratitudes, evidences, crises);
        List<DigitalTwinDto.Correlation>     correlations = detectCorrelations(lts, gratitudes, crises, logbooks);

        // ── Genera predicciones deterministas ────────────────────────────────
        List<FamilyPrediction> newPredictions = generatePredictions(familyId, lts, gratitudes, evidences, crises);

        // ── Genera firma conductual con IA ───────────────────────────────────
        String signature = generateBehavioralSignature(family, lts, patterns, correlations, dna, context, dataRichness);

        String dominantStrength     = extractDominantStrength(lts, patterns);
        String dominantVulnerability = extractDominantVulnerability(lts, crises);

        // ── Persiste el gemelo ───────────────────────────────────────────────
        FamilyTwinProfile twin = twinRepository.findByFamilyId(familyId)
                .orElse(FamilyTwinProfile.builder().familyId(familyId).build());

        twin.setBehavioralSignature(signature);
        twin.setCommunicationPattern(commPattern);
        twin.setResilienceIndex(resilienceIdx);
        twin.setBondingRhythm(bondingRhythm);
        twin.setDominantStrength(dominantStrength);
        twin.setDominantVulnerability(dominantVulnerability);
        twin.setDetectedPatterns(toJson(patterns));
        twin.setCorrelations(toJson(correlations));
        twin.setAvgDaysBetweenCrises(avgBetweenCrises);
        twin.setAvgRecoveryDays(avgRecovery);
        twin.setPeakActivityDay(peakDay);
        twin.setDataRichness(dataRichness);
        twin.setComputedAt(LocalDateTime.now());
        twinRepository.save(twin);

        // Reemplaza predicciones activas
        predictionRepository.deleteByFamilyIdAndStatus(familyId, "ACTIVE");
        predictionRepository.saveAll(newPredictions);

        log.info("[TWIN] Gemelo Digital actualizado — riqueza={} predicciones={}", dataRichness, newPredictions.size());
        return toDto(twin, family.getName(), newPredictions);
    }

    // ─── Predicciones deterministas ──────────────────────────────────────────

    private List<FamilyPrediction> generatePredictions(Long fid, FamilyLongitudinalState lts,
            List<FamilyGratitudeEntry> grat, List<TaskEvidence> evid, List<CriticalDay> crises) {

        List<FamilyPrediction> preds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. RIESGO DE TENSIÓN
        if (lts != null) {
            int detCount   = lts.getConsecutiveDeteriorations() != null ? lts.getConsecutiveDeteriorations() : 0;
            int crisis30d  = lts.getCrisisCount30d() != null ? lts.getCrisisCount30d() : 0;
            boolean collapse = Boolean.TRUE.equals(lts.getCommunicationCollapseActive());

            if (detCount >= 2 || crisis30d >= 1 || collapse) {
                int confidence = Math.min(95, 40 + detCount * 15 + crisis30d * 20 + (collapse ? 25 : 0));
                preds.add(FamilyPrediction.builder()
                    .familyId(fid)
                    .predictionType("TENSION_RISK")
                    .title("Riesgo de tensión en los próximos 14 días")
                    .description("Los patrones recientes sugieren que la familia podría enfrentar un período de mayor estrés relacional.")
                    .confidence(confidence)
                    .timeHorizon("próximos 14 días")
                    .recommendedAction("Activa el ritual semanal esta semana. Dedicar 20 minutos a escucharse sin agenda específica.")
                    .status("ACTIVE")
                    .predictedAt(now)
                    .expiresAt(now.plusDays(14))
                    .build());
            }
        }

        // 2. OPORTUNIDAD DE CRECIMIENTO
        if (lts != null) {
            int improvements = lts.getConsecutiveImprovements() != null ? lts.getConsecutiveImprovements() : 0;
            if (improvements >= 2) {
                preds.add(FamilyPrediction.builder()
                    .familyId(fid)
                    .predictionType("GROWTH_OPPORTUNITY")
                    .title("Ventana de crecimiento — momento ideal para un nuevo reto")
                    .description("La familia lleva " + improvements + " ciclos consecutivos de mejora. Este es el momento de introducir un nuevo desafío familiar.")
                    .confidence(Math.min(90, 50 + improvements * 10))
                    .timeHorizon("próximos 7 días")
                    .recommendedAction("Propone una nueva misión de Sprint que los lleve un paso más allá de la zona de confort actual.")
                    .status("ACTIVE")
                    .predictedAt(now)
                    .expiresAt(now.plusDays(7))
                    .build());
            }
        }

        // 3. ALERTA DE COMUNICACIÓN
        boolean commAlert = lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive());
        if (!commAlert && lts != null) {
            Double dimComm = lts.getDimComunicacion();
            if (dimComm != null && dimComm < 40.0) {
                preds.add(FamilyPrediction.builder()
                    .familyId(fid)
                    .predictionType("COMMUNICATION_ALERT")
                    .title("La dimensión comunicacional requiere atención")
                    .description("El indicador de comunicación familiar está en " + String.format("%.0f", dimComm) + "/100. Sin intervención, puede deteriorarse.")
                    .confidence(75)
                    .timeHorizon("próximas 3 semanas")
                    .recommendedAction("Usa el Consultor IA esta semana con el tema 'Comunicación'. Una conversación profunda puede cambiar el indicador.")
                    .status("ACTIVE")
                    .predictedAt(now)
                    .expiresAt(now.plusDays(21))
                    .build());
            }
        }

        // 4. LISTO PARA RITUAL
        long recentGrat = grat.stream()
                .filter(g -> g.getCreatedAt() != null && g.getCreatedAt().isAfter(now.minusDays(7))).count();
        long recentEvid = evid.stream()
                .filter(e -> e.getCreatedAt() != null && e.getCreatedAt().isAfter(now.minusDays(7))).count();
        if (recentGrat + recentEvid >= 5) {
            preds.add(FamilyPrediction.builder()
                .familyId(fid)
                .predictionType("RITUAL_READINESS")
                .title("La familia está lista para un ritual de celebración")
                .description("Con " + (recentGrat + recentEvid) + " eventos en los últimos 7 días, el nivel de conexión es ideal para un ritual.")
                .confidence(80)
                .timeHorizon("este fin de semana")
                .recommendedAction("Activa el Ritual del Domingo o el Ritual de Logro Celebrado en el Motor de Rituales.")
                .status("ACTIVE")
                .predictedAt(now)
                .expiresAt(now.plusDays(7))
                .build());
        }

        // 5. EVALUACIÓN PRÓXIMA
        if (lts != null && lts.getLastAssessmentAt() != null) {
            long daysSince = ChronoUnit.DAYS.between(lts.getLastAssessmentAt(), now);
            if (daysSince >= 80) {
                preds.add(FamilyPrediction.builder()
                    .familyId(fid)
                    .predictionType("EVALUATION_DUE")
                    .title("Diagnóstico trimestral pendiente")
                    .description("Han pasado " + daysSince + " días desde el último diagnóstico. El plan de transformación necesita re-calibrarse.")
                    .confidence(95)
                    .timeHorizon("próximos 10 días")
                    .recommendedAction("Agenda el diagnóstico familiar esta semana. Tomará 15 minutos y actualizará todos los planes automáticamente.")
                    .status("ACTIVE")
                    .predictedAt(now)
                    .expiresAt(now.plusDays(10))
                    .build());
            }
        }

        return preds;
    }

    // ─── Análisis de patrones ─────────────────────────────────────────────────

    private List<DigitalTwinDto.DetectedPattern> detectPatterns(
            FamilyLongitudinalState lts,
            List<FamilyGratitudeEntry> grat,
            List<TaskEvidence> evid,
            List<CriticalDay> crises) {

        List<DigitalTwinDto.DetectedPattern> patterns = new ArrayList<>();

        // Patrón: actividad concentrada en fin de semana
        long weekendEvents = countWeekendEvents(grat, evid);
        long totalEvents   = grat.size() + evid.size();
        if (totalEvents > 5 && weekendEvents * 100 / totalEvents > 60) {
            patterns.add(new DigitalTwinDto.DetectedPattern(
                "WEEKEND_BONDING", (int)(weekendEvents), 75,
                "Esta familia se conecta principalmente los fines de semana. El 60%+ de su actividad ocurre entre viernes y domingo."));
        }

        // Patrón: crisis estacional
        if (crises.size() >= 3) {
            patterns.add(new DigitalTwinDto.DetectedPattern(
                "CRISIS_CYCLE", crises.size(), 65,
                "Se han registrado " + crises.size() + " episodios de tensión. El gemelo está monitoreando su periodicidad."));
        }

        // Patrón: gratitud como mecanismo de recuperación
        if (!crises.isEmpty() && !grat.isEmpty()) {
            long gratAfterCrisis = crises.stream()
                    .filter(c -> c.getCreatedAt() != null)
                    .filter(c -> grat.stream().anyMatch(g -> g.getCreatedAt() != null
                            && g.getCreatedAt().isAfter(c.getCreatedAt())
                            && g.getCreatedAt().isBefore(c.getCreatedAt().plusDays(7))))
                    .count();
            if (gratAfterCrisis >= 1) {
                patterns.add(new DigitalTwinDto.DetectedPattern(
                    "GRATITUDE_AS_RECOVERY", (int)gratAfterCrisis, 80,
                    "Esta familia usa la gratitud como mecanismo de recuperación tras tensiones. Es una fortaleza significativa."));
            }
        }

        // Patrón: evidencias multimodales
        long photoEvid = evid.stream().filter(e -> EvidenceType.PHOTO.equals(e.getEvidenceType())).count();
        if (photoEvid > 3) {
            patterns.add(new DigitalTwinDto.DetectedPattern(
                "VISUAL_DOCUMENTATION", (int)photoEvid, 70,
                "Esta familia prefiere documentar sus momentos con fotos. Lo visual es su lenguaje de memoria natural."));
        }

        // Patrón de mejora si hay LTS
        if (lts != null && lts.getConsecutiveImprovements() != null && lts.getConsecutiveImprovements() >= 3) {
            patterns.add(new DigitalTwinDto.DetectedPattern(
                "SUSTAINED_IMPROVEMENT", lts.getConsecutiveImprovements(), 85,
                "La familia muestra " + lts.getConsecutiveImprovements() + " ciclos consecutivos de mejora. Patrón de resiliencia creciente."));
        }

        return patterns;
    }

    private List<DigitalTwinDto.Correlation> detectCorrelations(
            FamilyLongitudinalState lts,
            List<FamilyGratitudeEntry> grat,
            List<CriticalDay> crises,
            List<FamilyLogbookEntry> logs) {

        List<DigitalTwinDto.Correlation> corrs = new ArrayList<>();

        // Correlación: inactividad → deterioro
        if (lts != null && lts.getInactivityDays() != null && lts.getInactivityDays() > 7
                && lts.getConsecutiveDeteriorations() != null && lts.getConsecutiveDeteriorations() > 0) {
            corrs.add(new DigitalTwinDto.Correlation(
                "Períodos de inactividad", "Deterioro del ICF", 7, 72));
        }

        // Correlación: gratitudes → mejora comunicación
        if (!grat.isEmpty() && lts != null && lts.getConsecutiveImprovements() != null && lts.getConsecutiveImprovements() > 0) {
            corrs.add(new DigitalTwinDto.Correlation(
                "Práctica de gratitud", "Mejora en comunicación", 3, 68));
        }

        // Correlación: crisis → aumento de bitácora
        if (!crises.isEmpty() && !logs.isEmpty()) {
            corrs.add(new DigitalTwinDto.Correlation(
                "Registro de crisis", "Incremento de reflexión en bitácora", 2, 60));
        }

        return corrs;
    }

    // ─── Generación de firma conductual con IA ────────────────────────────────

    private String generateBehavioralSignature(Family family, FamilyLongitudinalState lts,
            List<DigitalTwinDto.DetectedPattern> patterns, List<DigitalTwinDto.Correlation> correlations,
            String dna, String context, String dataRichness) {
        try {
            String patternSummary = patterns.stream()
                    .map(p -> "• " + p.description())
                    .collect(Collectors.joining("\n"));

            String corrSummary = correlations.stream()
                    .map(c -> "• Cuando " + c.trigger().toLowerCase() + ", " + c.effect().toLowerCase()
                            + " ocurre ~" + c.lagDays() + " días después.")
                    .collect(Collectors.joining("\n"));

            String prompt = """
                Eres el motor de síntesis del Gemelo Digital Familiar de Integrity Family.
                Tu tarea es generar una "firma conductual" — una descripción de 3-4 oraciones
                que capture la personalidad operativa única de esta familia.

                FAMILIA: %s
                RIQUEZA DE DATOS: %s

                PATRONES DETECTADOS:
                %s

                CORRELACIONES:
                %s

                %s
                %s

                Genera UNA descripción de 3-4 oraciones que:
                1. Capture la esencia de cómo opera esta familia
                2. Mencione su ritmo natural, sus fortalezas y cómo responde a las tensiones
                3. Sea cálida y respetuosa — como lo haría un observador sabio que los conoce bien
                4. NO use tecnicismos psicológicos ni lenguaje clínico

                Responde solo con el texto, sin comillas ni formato.
                """.formatted(
                    family.getName(), dataRichness,
                    patternSummary.isEmpty() ? "Datos insuficientes para patrones claros aún." : patternSummary,
                    corrSummary.isEmpty() ? "Datos insuficientes para correlaciones." : corrSummary,
                    dna != null ? dna : "",
                    context != null ? context : ""
            );

            return aiProvider.generateRawResponse(prompt).strip();
        } catch (Exception e) {
            log.warn("[TWIN] Error generando firma conductual: {}", e.getMessage());
            return "Esta familia está construyendo su historia en Integrity Family. A medida que registren más momentos, el Gemelo Digital aprenderá su forma única de crecer juntos.";
        }
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private String computeDataRichness(List<?> grat, List<?> evid, List<?> log, List<?> crises) {
        int total = grat.size() + evid.size() + log.size() + crises.size();
        if (total >= 50) return "EXPERT";
        if (total >= 20) return "HIGH";
        if (total >= 8)  return "MEDIUM";
        return "LOW";
    }

    private double computeResilienceIndex(FamilyLongitudinalState lts, List<CriticalDay> crises) {
        if (lts == null || crises.isEmpty()) return 50.0;
        int avgRecovery = lts.getConsecutiveImprovements() != null ? lts.getConsecutiveImprovements() : 0;
        double base = Math.min(100.0, 40.0 + avgRecovery * 10.0);
        if (Boolean.TRUE.equals(lts.getCommunicationCollapseActive())) base -= 20;
        return Math.max(0, base);
    }

    private String computeBondingRhythm(List<FamilyGratitudeEntry> g, List<TaskEvidence> e, List<FamilyLogbookEntry> l) {
        int total = g.size() + e.size() + l.size();
        if (total == 0) return "UNKNOWN";
        // Estima el ritmo promedio
        if (!g.isEmpty() && g.size() > 20) return "DAILY";
        if (total > 10) return "WEEKLY";
        if (total > 3)  return "MONTHLY";
        return "SPORADIC";
    }

    private String computePeakDay(List<FamilyGratitudeEntry> g, List<TaskEvidence> e, List<FamilyLogbookEntry> l) {
        Map<DayOfWeek, Long> counts = new HashMap<>();
        g.forEach(x -> { if (x.getCreatedAt() != null) counts.merge(x.getCreatedAt().getDayOfWeek(), 1L, Long::sum); });
        e.forEach(x -> { if (x.getCreatedAt() != null) counts.merge(x.getCreatedAt().getDayOfWeek(), 1L, Long::sum); });
        l.forEach(x -> { if (x.getCreatedAt() != null) counts.merge(x.getCreatedAt().getDayOfWeek(), 1L, Long::sum); });
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e2 -> e2.getKey().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es")))
                .orElse(null);
    }

    private Integer computeAvgDaysBetweenCrises(List<CriticalDay> crises) {
        if (crises.size() < 2) return null;
        List<CriticalDay> sorted = crises.stream()
                .filter(c -> c.getCreatedAt() != null)
                .sorted(Comparator.comparing(CriticalDay::getCreatedAt))
                .toList();
        long totalDays = 0;
        for (int i = 1; i < sorted.size(); i++) {
            totalDays += ChronoUnit.DAYS.between(sorted.get(i-1).getCreatedAt(), sorted.get(i).getCreatedAt());
        }
        return (int)(totalDays / (sorted.size() - 1));
    }

    private Integer computeAvgRecoveryDays(FamilyLongitudinalState lts) {
        if (lts == null || lts.getCrisisCount30d() == null || lts.getCrisisCount30d() == 0) return null;
        // Estimación: si hay mejoras consecutivas después de crisis, la recuperación es relativamente rápida
        int improvements = lts.getConsecutiveImprovements() != null ? lts.getConsecutiveImprovements() : 0;
        return improvements > 0 ? Math.max(3, 14 - improvements * 2) : 14;
    }

    private long countWeekendEvents(List<FamilyGratitudeEntry> g, List<TaskEvidence> e) {
        long wg = g.stream().filter(x -> x.getCreatedAt() != null
                && (x.getCreatedAt().getDayOfWeek() == DayOfWeek.SATURDAY
                 || x.getCreatedAt().getDayOfWeek() == DayOfWeek.SUNDAY)).count();
        long we = e.stream().filter(x -> x.getCreatedAt() != null
                && (x.getCreatedAt().getDayOfWeek() == DayOfWeek.SATURDAY
                 || x.getCreatedAt().getDayOfWeek() == DayOfWeek.SUNDAY)).count();
        return wg + we;
    }

    private String extractDominantStrength(FamilyLongitudinalState lts, List<DigitalTwinDto.DetectedPattern> patterns) {
        if (!patterns.isEmpty()) return patterns.get(0).description().split("\\.")[0];
        if (lts != null && lts.getConsecutiveImprovements() != null && lts.getConsecutiveImprovements() > 0)
            return "Capacidad de mejora sostenida";
        return "En proceso de descubrimiento";
    }

    private String extractDominantVulnerability(FamilyLongitudinalState lts, List<CriticalDay> crises) {
        if (lts != null && Boolean.TRUE.equals(lts.getCommunicationCollapseActive()))
            return "Comunicación bajo presión";
        if (!crises.isEmpty()) return "Gestión de crisis relacionales";
        if (lts != null && lts.getCriticalDimension() != null)
            return "Dimensión " + lts.getCriticalDimension().toLowerCase();  // campo existe en LTS
        return "Por descubrir con más historial";
    }

    // ─── Bloque de contexto para IA ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildTwinContextBlock(Long familyId) {
        try {
            return twinRepository.findByFamilyId(familyId).map(twin -> {
                StringBuilder sb = new StringBuilder("Gemelo Digital:\n");
                if (twin.getBehavioralSignature() != null)
                    sb.append("  Firma conductual: ").append(twin.getBehavioralSignature()).append("\n");
                if (twin.getDominantStrength() != null)
                    sb.append("  Fortaleza dominante: ").append(twin.getDominantStrength()).append("\n");
                if (twin.getDominantVulnerability() != null)
                    sb.append("  Vulnerabilidad dominante: ").append(twin.getDominantVulnerability()).append("\n");
                if (twin.getResilienceIndex() != null)
                    sb.append("  Índice de resiliencia: ").append(String.format("%.0f", twin.getResilienceIndex())).append("/100\n");
                return sb.toString();
            }).orElse(null);
        } catch (Exception e) {
            log.warn("[TWIN] Error construyendo bloque de contexto: {}", e.getMessage());
            return null;
        }
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────

    private DigitalTwinDto toDto(FamilyTwinProfile twin, String familyName, List<FamilyPrediction> predictions) {
        return new DigitalTwinDto(
                twin.getFamilyId(), familyName,
                twin.getBehavioralSignature(),
                twin.getCommunicationPattern(),
                twin.getResilienceIndex() != null ? twin.getResilienceIndex() : 50.0,
                twin.getBondingRhythm(),
                twin.getDominantStrength(),
                twin.getDominantVulnerability(),
                twin.getDataRichness(),
                twin.getAvgDaysBetweenCrises(),
                twin.getAvgRecoveryDays(),
                twin.getPeakActivityDay(),
                parsePatterns(twin.getDetectedPatterns()),
                parseCorrelations(twin.getCorrelations()),
                predictions.stream().map(p -> new DigitalTwinDto.PredictionDto(
                        p.getId(), p.getPredictionType(), p.getTitle(),
                        p.getDescription(), p.getConfidence(), p.getTimeHorizon(),
                        p.getRecommendedAction(), p.getStatus()
                )).collect(Collectors.toList()),
                twin.getComputedAt()
        );
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private List<DigitalTwinDto.DetectedPattern> parsePatterns(String json) {
        if (json == null) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    private List<DigitalTwinDto.Correlation> parseCorrelations(String json) {
        if (json == null) return Collections.emptyList();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return Collections.emptyList(); }
    }
}
