package com.integrityfamily.assessment.service;

import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.domain.DimensionType;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationAnswer;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servicio de respuestas incrementales — flujo mobile-first.
 *
 * Permite guardar respuestas una a una (o en lote) mientras el usuario avanza
 * por el cuestionario. Si cierra la app, al volver puede retomar desde donde
 * se quedó consultando {@link #getProgress(Long)}.
 *
 * Upsert idempotente: si una pregunta ya fue respondida, actualiza su score
 * en lugar de crear una fila duplicada (respaldado por el UNIQUE KEY en BD).
 *
 * Reglas de valor:
 *   - Escala 1-5  → se usa directamente
 *   - Sí/No       → true=5, false=1 (compatible con RISK_ALGO_V1)
 *   - Sin ninguno → valor neutro 3 (no debería ocurrir en producción)
 *
 * El método {@link #loadAnswersAsDto(Long)} permite a {@code EvaluationService.finalize()}
 * recuperar las respuestas guardadas cuando el cuerpo del request llega vacío.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentAnswerService {

    private static final int MIN_ANSWERS_TO_FINALIZE = 10;
    private static final int STANDARD_QUESTIONNAIRE_SIZE = 20;

    private final EvaluationRepository       evaluationRepository;
    private final EvaluationAnswerRepository  answerRepository;
    private final QuestionRepository          questionRepository;

    // ─── API Pública ──────────────────────────────────────────────────────────

    /**
     * Guarda una o varias respuestas de forma incremental.
     * Hace upsert por (evaluation_id, question_key): actualiza si ya existía.
     *
     * @param evalId   ID de la evaluación activa (STARTED)
     * @param requests lista de respuestas a persistir
     * @return progreso actualizado del cuestionario
     */
    @Transactional
    public EvaluationDtos.AnswerProgressResponse saveAnswers(
            Long evalId,
            List<EvaluationDtos.SaveAnswerRequest> requests) {

        Evaluation eval = findActiveEvaluation(evalId);

        // ── Batch-load 1/3: preguntas en una sola query ─────────────────────────
        List<Long> questionIds = requests.stream()
                .map(EvaluationDtos.SaveAnswerRequest::questionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // ── Batch-load 2/3: respuestas ya existentes para upsert ────────────────
        Map<String, EvaluationAnswer> existingByKey =
                answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(evalId).stream()
                        .collect(Collectors.toMap(EvaluationAnswer::getQuestionKey, a -> a));

        int saved   = 0;
        int updated = 0;
        List<EvaluationAnswer> toSave = new ArrayList<>();

        for (EvaluationDtos.SaveAnswerRequest req : requests) {
            if (req.questionId() == null) continue;

            Question q = questionMap.get(req.questionId());
            if (q == null) {
                log.warn("[ANSWER-SERVICE] Pregunta no encontrada: questionId={} (ignorada)", req.questionId());
                continue;
            }

            String qKey = q.getQuestionKey() != null ? q.getQuestionKey() : "Q-" + q.getId();

            // Upsert sin SELECT por fila: usamos el mapa pre-cargado
            EvaluationAnswer answer = existingByKey.computeIfAbsent(qKey, key -> {
                EvaluationAnswer a = new EvaluationAnswer();
                a.setEvaluation(eval);
                a.setQuestionKey(key);
                a.setQuestionId(q.getId());
                return a;
            });

            boolean isNew = answer.getId() == null;

            answer.setScore(req.effectiveScore());
            answer.setBooleanAnswer(req.booleanAnswer());
            answer.setAnsweredAt(LocalDateTime.now());
            answer.setDimension(mapDimension(q.getDimension()));
            answer.setDiagnosticDimension(q.getDimension());
            answer.setConsciousnessLevel(resolveConsciousnessLevel(req.effectiveScore()));

            toSave.add(answer);
            if (isNew) saved++; else updated++;
        }

        // ── Batch-save 3/3: persistir todas las respuestas en una pasada ────────
        answerRepository.saveAll(toSave);

        long total = answerRepository.countByEvaluationId(evalId);
        log.info("[ANSWER-SERVICE] evalId={} | nuevas={} | actualizadas={} | total={}",
                evalId, saved, updated, total);

        return buildProgress(eval.getId(), total);
    }

    /**
     * Devuelve el estado de progreso actual: cuántas respondidas, lista para reanudar.
     *
     * @param evalId ID de la evaluación
     */
    @Transactional(readOnly = true)
    public EvaluationDtos.AnswerProgressResponse getProgress(Long evalId) {
        // Permite consultar incluso si ya fue finalizada (para mostrar resumen)
        Evaluation eval = evaluationRepository.findById(evalId)
                .orElseThrow(() -> new NotFoundException("Evaluación no encontrada: " + evalId));

        long total = answerRepository.countByEvaluationId(evalId);
        return buildProgress(eval.getId(), total);
    }

    /**
     * Carga las respuestas guardadas como {@code AnswerDto} para ser consumidas
     * por {@code EvaluationService.finalize()} cuando el cuerpo del request llega vacío.
     * Solo incluye filas que tengan questionId (garantiza que el engine pueda cargarlas).
     */
    @Transactional(readOnly = true)
    public List<EvaluationDtos.AnswerDto> loadAnswersAsDto(Long evalId) {
        return answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(evalId).stream()
                .filter(a -> a.getQuestionId() != null)
                .map(a -> new EvaluationDtos.AnswerDto(a.getQuestionId(), a.getScore(), null))
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Evaluation findActiveEvaluation(Long evalId) {
        Evaluation eval = evaluationRepository.findById(evalId)
                .orElseThrow(() -> new NotFoundException("Evaluación no encontrada: " + evalId));
        if (eval.getStatus() == EvaluationStatus.FINALIZED) {
            throw new IllegalStateException(
                    "No se pueden guardar respuestas: la evaluación " + evalId + " ya fue finalizada.");
        }
        return eval;
    }

    private EvaluationDtos.AnswerProgressResponse buildProgress(Long evalId, long answered) {
        List<EvaluationDtos.SavedAnswerDto> list =
                answerRepository.findByEvaluationIdOrderByAnsweredAtAsc(evalId).stream()
                        .map(a -> new EvaluationDtos.SavedAnswerDto(
                                a.getQuestionId(),
                                a.getQuestionKey(),
                                a.getScore(),
                                a.getBooleanAnswer(),
                                a.getDimension() != null ? a.getDimension().name().toLowerCase() : null,
                                a.getDiagnosticDimension(),
                                a.getConsciousnessLevel(),
                                a.getAnsweredAt()))
                        .toList();

        boolean canFinalize = answered >= MIN_ANSWERS_TO_FINALIZE;

        return new EvaluationDtos.AnswerProgressResponse(
                evalId,
                (int) answered,
                STANDARD_QUESTIONNAIRE_SIZE,
                canFinalize,
                list);
    }

    /**
     * Mapea el nombre de dimensión del banco de preguntas a {@code DimensionType}.
     * Las dimensiones v2 (emociones / comunicacion / habitos / tiempos) no existen
     * en el enum legacy, así que caen a {@code COMMITMENT} — valor neutral aceptado
     * por toda la capa de persistencia.
     */
    private DimensionType mapDimension(String raw) {
        if (raw == null) return DimensionType.COMMITMENT;
        try {
            return DimensionType.valueOf(raw.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return DimensionType.COMMITMENT;
        }
    }

    /**
     * Mapea la puntuación numérica 1-5 a la fase de conciencia en español.
     */
    private String resolveConsciousnessLevel(int score) {
        return switch (score) {
            case 1 -> "Inconsciente";
            case 2 -> "Reactivo";
            case 3 -> "Consciente";
            case 4 -> "Intencional";
            case 5 -> "Pleno";
            default -> "Consciente";
        };
    }
}
