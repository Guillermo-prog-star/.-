package com.integrityfamily.ai.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comentario asíncrono de la IA sobre una evidencia enviada por la familia.
 *
 * <p>La IA <b>no califica, no aprueba y no rechaza</b> evidencias. Antes este
 * consumidor asignaba un puntaje 0–100 y validaba o rechazaba solo (≥ 70), lo
 * que convertía un resultado desfavorable en "incumplimiento" y un texto breve
 * en "incoherencia conductual". Ahora solo envía a la familia un comentario
 * descriptivo como notificación:
 * <ul>
 *   <li>no modifica el texto que escribió la familia ({@code description});</li>
 *   <li>no escribe {@code aiScore} ni cambia el estado de la evidencia o de la tarea;</li>
 *   <li>la validación, si se usa, sigue siendo humana ({@code POST /api/evidences/{id}/validate|reject});</li>
 *   <li>la ubicación de la evidencia no se envía al proveedor de IA.</li>
 * </ul>
 * Si la IA falla no se reintenta: el comentario es opcional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceAnalysisConsumer {

    static final String NOTIFICATION_TYPE = "EVIDENCE_FEEDBACK";
    private static final int MAX_FEEDBACK_CHARS = 400;

    private final TaskEvidenceRepository taskEvidenceRepository;
    private final AiProvider aiProvider;
    private final UserNotificationService userNotificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional // push() persiste la notificación
    @RabbitListener(queues = RabbitConfig.EVIDENCE_ANALYSIS_QUEUE)
    public void onEvidenceSubmitted(SystemEvent event) {
        try {
            Long evidenceId = extractEvidenceId(event.payload());
            if (evidenceId == null) {
                log.error("❌ [EVIDENCE-AI] El payload del evento no contiene un ID de evidencia válido.");
                return;
            }

            TaskEvidence evidence = taskEvidenceRepository.findById(evidenceId).orElse(null);
            if (evidence == null) {
                log.warn("⚠️ [EVIDENCE-AI] Evidencia ID '{}' no encontrada. Sin comentario.", evidenceId);
                return;
            }
            PlanTask task = evidence.getTask();
            if (task == null) {
                log.warn("⚠️ [EVIDENCE-AI] Evidencia ID '{}' sin tarea asociada. Sin comentario.", evidenceId);
                return;
            }

            String feedback = parseFeedback(aiProvider.generateRawResponse(buildPrompt(task, evidence)));
            if (feedback == null) {
                log.warn("⚠️ [EVIDENCE-AI] Respuesta sin comentario utilizable para evidencia ID '{}'.", evidenceId);
                return;
            }

            userNotificationService.push(
                    evidence.getFamily(), null,
                    NOTIFICATION_TYPE,
                    "Un comentario sobre su registro",
                    String.format("Sobre \"%s\": %s", task.getTitle(), feedback)
            );
            log.info("💬 [EVIDENCE-AI] Comentario enviado para evidencia ID '{}'.", evidenceId);

        } catch (Exception e) {
            // El comentario es opcional: no se propaga para no reencolar el mensaje indefinidamente.
            log.error("❌ [EVIDENCE-AI] No se pudo generar el comentario de la evidencia: {}", e.getMessage(), e);
        }
    }

    private static Long extractEvidenceId(Object payload) {
        if (payload instanceof Number n) return n.longValue();
        if (payload instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    static String buildPrompt(PlanTask task, TaskEvidence evidence) {
        return String.format("""
            Eres el acompañante de Integrity Family. Una familia registró cómo le fue con una práctica de su plan.
            Escribe un comentario breve para la familia.

            PRÁCTICA:
            - Título: %s
            - Acción propuesta: %s
            - Dimensión: %s

            LO QUE REGISTRÓ LA FAMILIA:
            - Título: %s
            - Descripción: %s
            - Texto / reflexión: %s
            - Emoción reportada: %s

            REGLAS:
            1. No califiques, no puntúes y no digas si la práctica "se cumplió", "se aprobó" o "es coherente".
               No eres quien valida el registro.
            2. Un resultado desfavorable o un registro breve no es una ejecución incorrecta ni un fracaso.
            3. Describe la conducta o la situación, nunca a la persona. No uses etiquetas ni diagnósticos
               ("resistente", "evitativo", "incoherente", "irresponsable") ni infieras estados cerebrales o clínicos.
            4. Reconoce una cosa concreta que la familia contó y haz como máximo UNA pregunta abierta que
               ayude a revisar o ajustar la práctica (por ejemplo, qué la dificultó o qué cambiarían).
            5. Si el texto menciona violencia, amenaza, autolesión o riesgo para alguien, no comentes la práctica:
               invita a buscar apoyo inmediato y a usar el protocolo de seguridad de la plataforma.
            6. Español, tono cercano, máximo 400 caracteres.

            Responde ÚNICAMENTE con un objeto JSON: {"feedback": "..."}
            """,
            orDefault(task.getTitle(), "Sin título"),
            orDefault(task.getAccionConcreta(), "Sin acción descrita"),
            orDefault(task.getDimension(), "General"),
            orDefault(evidence.getTitle(), "Sin título"),
            orDefault(evidence.getDescription(), "Sin descripción"),
            orDefault(evidence.getTextContent(), "Sin texto"),
            orDefault(evidence.getEmotion(), "No especificada")
        );
    }

    String parseFeedback(String raw) {
        if (raw == null) return null;
        String json = raw.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();
        try {
            JsonNode node = objectMapper.readTree(json);
            String feedback = node.path("feedback").asText("").trim();
            if (feedback.isEmpty()) return null;
            return feedback.length() > MAX_FEEDBACK_CHARS ? feedback.substring(0, MAX_FEEDBACK_CHARS) : feedback;
        } catch (Exception e) {
            return null;
        }
    }

    private static String orDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
