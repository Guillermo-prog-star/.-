package com.integrityfamily.ai.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.checklist.service.TaskEvidenceService;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * SDD SPEC 6.9: Consumidor de Eventos para el Análisis Cognitivo Asíncrono de Evidencias.
 * Integra la evaluación clínica de Claude con la automatización del flujo conductual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceAnalysisConsumer {

    private final TaskEvidenceRepository taskEvidenceRepository;
    private final TaskEvidenceService taskEvidenceService;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = RabbitConfig.EVIDENCE_ANALYSIS_QUEUE)
    public void onEvidenceSubmitted(SystemEvent event) {
        log.info("🤖 [SENTINEL-AI] Iniciando análisis cognitivo asíncrono para evento de evidencia de la familia: {}", event.familyId());

        try {
            // 1. Extraer ID de la evidencia de manera segura
            Long evidenceId = null;
            if (event.payload() instanceof Number) {
                evidenceId = ((Number) event.payload()).longValue();
            } else if (event.payload() instanceof String) {
                evidenceId = Long.parseLong((String) event.payload());
            }

            if (evidenceId == null) {
                log.error("❌ [SENTINEL-AI] El payload del evento no contiene un ID de evidencia válido.");
                return;
            }

            // 2. Cargar evidencia de la base de datos
            TaskEvidence evidence = taskEvidenceRepository.findById(evidenceId).orElse(null);
            if (evidence == null) {
                log.warn("⚠️ [SENTINEL-AI] Evidencia con ID '{}' no encontrada en base de datos. Abortando análisis.", evidenceId);
                return;
            }

            PlanTask task = evidence.getTask();
            if (task == null) {
                log.warn("⚠️ [SENTINEL-AI] Tarea asociada a la evidencia ID '{}' es nula. Abortando.", evidenceId);
                return;
            }

            log.info("🔍 [SENTINEL-AI] Evidencia ID '{}' cargada. Tarea: '{}' | Familia: '{}'", 
                    evidenceId, task.getTitle(), evidence.getFamily().getName());

            // 3. Construir el Prompt Clínico para Claude
            String prompt = String.format("""
                Eres Sentinel AI, el motor de inteligencia artificial clínica y conductual de la plataforma Integrity Family.
                Tu tarea es evaluar la coherencia, asimilación conductual y autenticidad de la evidencia suministrada por una familia para una misión (tarea) de su Plan de Transformación.
                
                INFORMACIÓN DE LA MISIÓN:
                - Título de la Misión: %s
                - Acción Concreta Requerida: %s
                - Indicador de Cumplimiento: %s
                - Evidencia Solicitada: %s
                - Dimensión: %s
                
                EVIDENCIA SUMINISTRADA POR LA FAMILIA:
                - Tipo de Evidencia: %s
                - Título del Envío: %s
                - Descripción: %s
                - Contenido de Texto / Reflexión: %s
                
                Por favor, realiza un análisis conductual riguroso enfocado en la asimilación real de la dinámica de cambio familiar.
                Determina si lo enviado demuestra de forma creíble que se ha ejecutado la acción concreta y se cumple con el indicador solicitado.
                
                Debes responder ÚNICAMENTE con un objeto JSON válido que contenga la siguiente estructura (no agregues texto de introducción o de despedida, solo responde con el objeto JSON):
                {
                  "score": (un número decimal entre 0.0 y 100.0 que mida el grado de cumplimiento),
                  "coherence": (booleano true si el score es >= 70.0, false de lo contrario),
                  "feedback": "Tu análisis clínico constructivo detallado en español dirigido a la familia (máximo 400 caracteres)"
                }
                """,
                task.getTitle() != null ? task.getTitle() : "Sin título",
                task.getAccionConcreta() != null ? task.getAccionConcreta() : "Sin acción concreta",
                task.getIndicadorCumplimiento() != null ? task.getIndicadorCumplimiento() : "Sin indicador",
                task.getEvidenciaRequerida() != null ? task.getEvidenciaRequerida() : "Sin evidencia requerida",
                task.getDimension() != null ? task.getDimension() : "General",
                evidence.getEvidenceType().name(),
                evidence.getTitle() != null ? evidence.getTitle() : "Sin título",
                evidence.getDescription() != null ? evidence.getDescription() : "Sin descripción",
                evidence.getTextContent() != null ? evidence.getTextContent() : "Sin contenido de texto"
            );

            // 4. Invocar a Claude de forma síncrona dentro de este hilo asíncrono
            log.info("🤖 [SENTINEL-AI] Invocando motor de Claude para análisis cognitivo...");
            String jsonResponse = aiProvider.generateRawResponse(prompt).trim();

            // Limpiar bloques de markdown si la IA los incluye
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.replaceAll("^```json\\s*", "");
                jsonResponse = jsonResponse.replaceAll("^```\\s*", "");
                jsonResponse = jsonResponse.replaceAll("\\s*```$", "");
                jsonResponse = jsonResponse.trim();
            }

            log.info("🤖 [SENTINEL-AI] Respuesta de Claude recibida: {}", jsonResponse);

            // 5. Parsear el resultado JSON de Claude
            JsonNode node = objectMapper.readTree(jsonResponse);
            double score = node.path("score").asDouble(0.0);
            boolean coherence = node.path("coherence").asBoolean(false);
            String feedback = node.path("feedback").asText("Evidencia procesada por el motor de inteligencia artificial.");

            // 6. Sellar la Evidencia (Validar o Rechazar automáticamente)
            evidence.setAiScore(score);
            evidence.setDescription("[Retroalimentación de Sentinel AI: " + feedback + "]\n\n" + 
                    (evidence.getDescription() != null ? evidence.getDescription() : ""));
            taskEvidenceRepository.save(evidence);

            if (coherence) {
                taskEvidenceService.validateEvidence(evidenceId, score, "Sentinel AI");
                log.info("✅ [SENTINEL-AI] Evidencia ID '{}' aprobada con éxito. Score: {}", evidenceId, score);
            } else {
                taskEvidenceService.rejectEvidence(evidenceId, "Sentinel AI");
                log.warn("❌ [SENTINEL-AI] Evidencia ID '{}' desaprobada por incoherencia conductual. Score: {}", evidenceId, score);
            }

        } catch (Exception e) {
            log.error("❌ [SENTINEL-AI] Error crítico durante el análisis asíncrono de la evidencia: {}", e.getMessage(), e);
            throw new RuntimeException("Fallo en EvidenceAnalysisConsumer al procesar análisis cognitivo", e);
        }
    }
}
