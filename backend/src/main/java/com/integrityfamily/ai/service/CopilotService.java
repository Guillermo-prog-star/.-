package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.CopilotDtos.*;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * SDD Sprint 6: Servicio de Copiloto Estructurado e Inteligencia Híbrida.
 * Orquesta la construcción de contexto compacto, inferencia estructurada de Claude y auditoría de gobernanza.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final PlanTaskRepository planTaskRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;
    private final LearningEntryRepository learningEntryRepository;
    private final FamilyMetricsSnapshotRepository snapshotRepository;
    private final AiInferenceRepository inferenceRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CompactFamilyContext buildContext(Long familyId) {
        log.info("🧩 [CONTEXT BUILDER] Construyendo resumen estructurado compacto para familia ID: {}", familyId);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada: " + familyId));

        // Nivel de Riesgo y Dimensión
        List<Evaluation> evals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId);
        Evaluation latestEval = evals.isEmpty() ? null : evals.get(evals.size() - 1);

        String riskLevel = latestEval != null && latestEval.getRiskLevel() != null ? latestEval.getRiskLevel() : "MODERATE";
        String criticalDimension = latestEval != null && latestEval.getCriticalDimension() != null ? latestEval.getCriticalDimension() : "COMUNICACION";

        // Tendencia
        List<FamilyMetricsSnapshot> history = snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId);
        String trend = "ESTABLE";
        if (history.size() >= 2) {
            Double latestIndex = history.get(history.size() - 1).getConvivenceIndex();
            Double oldestIndex = history.get(0).getConvivenceIndex();
            if (latestIndex != null && oldestIndex != null) {
                if (latestIndex < oldestIndex - 5) trend = "WORSENING";
                else if (latestIndex > oldestIndex + 5) trend = "IMPROVING";
            }
        }

        // Adherencia
        List<PlanTask> tasks = planTaskRepository.findAll().stream()
                .filter(t -> t.getPlan() != null && t.getPlan().getFamily() != null && t.getPlan().getFamily().getId().equals(familyId))
                .toList();
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(PlanTask::isCompleted).count();
        double adherence = totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100.0 : 50.0;

        // Días Inactivos
        List<TaskEvidence> evidences = taskEvidenceRepository.findAll().stream()
                .filter(e -> e.getFamily() != null && e.getFamily().getId().equals(familyId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        int inactiveDays = 15;
        if (!evidences.isEmpty()) {
            inactiveDays = (int) ChronoUnit.DAYS.between(evidences.get(0).getCreatedAt(), LocalDateTime.now());
        }

        // Aprendizajes Recientes
        List<String> learnings = learningEntryRepository.findByFamilyId(familyId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(3)
                .map(LearningEntry::getAwarenessShift)
                .toList();
        if (learnings.isEmpty()) {
            learnings = List.of("Dificultad para escucharse", "Interrupciones frecuentes");
        }

        // Alertas Activas
        List<String> alerts = new ArrayList<>();
        if (adherence < 40) alerts.add("LOW_ADHERENCE");
        if (inactiveDays > 14) alerts.add("INACTIVITY");
        if ("WORSENING".equals(trend)) alerts.add("COMMUNICATION_REGRESSION");

        return CompactFamilyContext.builder()
                .familyId(familyId)
                .riskLevel(riskLevel)
                .criticalDimension(criticalDimension)
                .trend(trend)
                .adherence(adherence)
                .inactiveDays(inactiveDays)
                .recentLearnings(learnings)
                .alerts(alerts)
                .build();
    }

    @Transactional
    public StructuredAiInferenceResponse generateInference(CopilotInferRequest req) {
        log.info("🧠 [AI COPILOT] Generando inferencia híbrida para familia ID: {}", req.familyId());
        CompactFamilyContext context = buildContext(req.familyId());

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            contextJson = context.toString();
        }

        String prompt = String.format(
            "Actúa como motor de interpretación contextual de convivencia familiar (sin diagnosticar trastornos ni emitir juicios morales). " +
            "Analiza el siguiente contexto estructurado compacto y devuelve UNICAMENTE un JSON válido con esta estructura exacta: " +
            "{\"summary\": \"resumen de la situación\", \"priority\": \"HIGH|MEDIUM|LOW\", \"recommendedActions\": [\"acción 1\", \"acción 2\"], \"containmentSuggestion\": \"sugerencia proactiva\", \"followUpDays\": 7}. " +
            "Contexto familiar: %s", contextJson
        );

        String rawResponse;
        try {
            rawResponse = aiProvider.generateRawResponse(prompt);
        } catch (Exception e) {
            log.error("⚠️ Error llamando a Claude, aplicando fallback determinístico: {}", e.getMessage());
            rawResponse = "";
        }

        StructuredAiInferenceResponse structuredResponse;
        try {
            // Limpiar posibles bloques de markdown
            String cleanJson = rawResponse.replace("```json", "").replace("```", "").trim();
            structuredResponse = objectMapper.readValue(cleanJson, StructuredAiInferenceResponse.class);
        } catch (Exception e) {
            log.warn("⚠️ Fallo parseando JSON de Claude, generando sugerencia proactiva de respaldo según reglas duras.");
            
            String summary;
            String priority = "MEDIUM";
            String containment;
            List<String> actions;

            if (context.adherence() < 40) {
                summary = "La familia muestra baja adherencia y posible fatiga operacional.";
                priority = "HIGH";
                containment = "La familia podría estar saturada por exceso de tareas. Reducir complejidad y reiniciar con actividades cortas.";
                actions = List.of("Pausar misiones de 3 y 6 meses", "Asignar misión de 1 semana: 'Cena sin celulares'");
            } else if ("WORSENING".equals(context.trend())) {
                summary = "Existe deterioro sostenido en comunicación familiar.";
                priority = "HIGH";
                containment = "Priorizar actividades de escucha breve antes de conversaciones profundas.";
                actions = List.of("Activar tareas cortas de escucha activa", "Incrementar frecuencia de reflexión guiada");
            } else {
                summary = "La familia mantiene adherencia estable y mejora evolutiva gradual.";
                priority = "LOW";
                containment = "Consolidar hábitos actuales antes de aumentar intensidad de intervención.";
                actions = List.of("Mantener calendario actual", "Celebrar racha continua");
            }

            structuredResponse = StructuredAiInferenceResponse.builder()
                    .summary(summary)
                    .priority(priority)
                    .recommendedActions(actions)
                    .containmentSuggestion(containment)
                    .followUpDays(7)
                    .build();
        }

        // SDD SPEC: Gobernanza IA (Persistir prompt, contexto y respuesta inmutablemente)
        String outputJson;
        try {
            outputJson = objectMapper.writeValueAsString(structuredResponse);
        } catch (Exception e) {
            outputJson = structuredResponse.toString();
        }

        AiInferenceEntity inferenceEntity = AiInferenceEntity.builder()
                .familyId(req.familyId())
                .contextHash(Integer.toHexString(contextJson.hashCode()))
                .inputSummary(contextJson)
                .inferenceResult(outputJson)
                .priority(structuredResponse.priority())
                .promptUsed(prompt)
                .modelVersion("claude-3-5-sonnet")
                .createdAt(LocalDateTime.now())
                .build();
        inferenceRepository.save(inferenceEntity);

        return structuredResponse;
    }

    @Transactional(readOnly = true)
    public StructuredAiInferenceResponse getLatestSuggestion(Long familyId) {
        AiInferenceEntity latest = inferenceRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId);
        if (latest == null || latest.getInferenceResult() == null) {
            return generateInference(new CopilotInferRequest(familyId, "INITIAL_LOAD"));
        }
        try {
            return objectMapper.readValue(latest.getInferenceResult(), StructuredAiInferenceResponse.class);
        } catch (Exception e) {
            return generateInference(new CopilotInferRequest(familyId, "RECALCULATE"));
        }
    }

    @Transactional(readOnly = true)
    public List<AiInferenceEntity> getHistory(Long familyId) {
        return inferenceRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }
}
