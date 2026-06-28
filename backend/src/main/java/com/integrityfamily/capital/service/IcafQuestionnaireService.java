package com.integrityfamily.capital.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyIcafAnswer;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de cuestionarios ICaF (confianza, bienestar_emocional, ...).
 *
 * Flujo:
 *   1. El frontend llama getQuestions(domain) para obtener los ítems a mostrar.
 *   2. El usuario responde (escala 1-5) y el frontend envía saveAnswers().
 *   3. IcafDomainResolver consulta el score promedio vía FamilyIcafAnswerRepository.
 *   4. El próximo cálculo del ICaF usa el score real en lugar de la estimación.
 *
 * Reglas de normalización (escala 1-5 → 0-100):
 *   POSITIVE: (score - 1) / 4 × 100
 *   NEGATIVE: (5 - score) / 4 × 100  (pregunta invertida)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcafQuestionnaireService {

    public static final String DOMAIN_CONFIANZA      = "confianza";
    public static final String DOMAIN_BIENESTAR      = "bienestar_emocional";
    public static final String DOMAIN_AUTONOMIA      = "autonomia";
    public static final String DOMAIN_PROPOSITO      = "proposito";
    public static final String DOMAIN_EMPRENDIMIENTO = "emprendimiento";
    public static final String DOMAIN_LEGADO         = "legado";
    public static final String QUESTION_TYPE_ICAF = "ICAF";

    private final QuestionRepository         questionRepo;
    private final FamilyIcafAnswerRepository answerRepo;
    private final FamilyRepository           familyRepo;

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Devuelve los ítems activos de un dominio ICaF ordenados por sortOrder. */
    public List<Question> getQuestions(String icafDomain) {
        return questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                QUESTION_TYPE_ICAF, icafDomain);
    }

    /**
     * Devuelve las respuestas actuales de la familia para un dominio.
     * Útil para mostrar el estado previo al re-responder.
     */
    public List<FamilyIcafAnswer> getCurrentAnswers(Long familyId, String icafDomain) {
        return answerRepo.findByFamilyIdAndIcafDomain(familyId, icafDomain);
    }

    /**
     * Devuelve el score normalizado del dominio para una familia (0-100).
     * Si la familia no ha respondido, devuelve empty.
     */
    public Optional<Double> getDomainScore(Long familyId, String icafDomain) {
        if (!answerRepo.hasAnswers(familyId, icafDomain)) return Optional.empty();
        double rawAvg = answerRepo.avgScoreByDomain(familyId, icafDomain);
        return Optional.of(normalize(rawAvg, "POSITIVE"));
    }

    // ── Guardar respuestas ────────────────────────────────────────────────────

    /**
     * UPSERT de respuestas. Una fila por (family_id, question_key).
     * Si la familia ya respondió, actualiza el score.
     *
     * @param familyId   ID de la familia
     * @param answers    Map de questionKey → score (1-5)
     * @param answeredBy email del miembro respondiente (null = respuesta conjunta)
     * @return score normalizado del dominio tras guardar
     */
    @Transactional
    public DomainScoreResult saveAnswers(Long familyId, Map<String, Integer> answers,
                                         String icafDomain, String answeredBy) {
        Family family = familyRepo.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        // Cargar preguntas del dominio para validar y obtener direction
        Map<String, Question> questionMap = buildQuestionMap(icafDomain);

        int saved = 0;
        for (Map.Entry<String, Integer> entry : answers.entrySet()) {
            String questionKey = entry.getKey();
            Integer score      = entry.getValue();

            if (score < 1 || score > 5) {
                log.warn("[ICaF-Q] Score fuera de rango para {}: {} — ignorado", questionKey, score);
                continue;
            }

            Optional<FamilyIcafAnswer> existing = answerRepo.findByFamilyIdAndQuestionKey(familyId, questionKey);
            if (existing.isPresent()) {
                existing.get().setScore(score);
                existing.get().setAnsweredBy(answeredBy);
                answerRepo.save(existing.get());
            } else {
                FamilyIcafAnswer answer = FamilyIcafAnswer.builder()
                        .family(family)
                        .questionKey(questionKey)
                        .icafDomain(icafDomain)
                        .score(score)
                        .answeredBy(answeredBy)
                        .build();
                answerRepo.save(answer);
            }
            saved++;
        }

        // Recalcular score del dominio con dirección correcta por pregunta
        double domainScore = computeWeightedScore(familyId, icafDomain, questionMap);

        log.info("[ICaF-Q] Familia {} | dominio={} | {} respuestas guardadas | score={}",
                familyId, icafDomain, saved, String.format("%.1f", domainScore));

        return new DomainScoreResult(icafDomain, domainScore, saved,
                answerRepo.countAnsweredByDomain(familyId, icafDomain));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Score ponderado respetando direction POSITIVE/NEGATIVE por pregunta.
     * Si no hay mapa de preguntas (preguntas desconocidas), usa promedio simple.
     */
    private double computeWeightedScore(Long familyId, String icafDomain,
                                         Map<String, Question> questionMap) {
        List<FamilyIcafAnswer> answers = answerRepo.findByFamilyIdAndIcafDomain(familyId, icafDomain);
        if (answers.isEmpty()) return 0.0;

        double total = 0.0;
        for (FamilyIcafAnswer a : answers) {
            Question q = questionMap.get(a.getQuestionKey());
            String direction = (q != null && "NEGATIVE".equals(q.getDirection())) ? "NEGATIVE" : "POSITIVE";
            total += normalize(a.getScore(), direction);
        }
        return Math.round((total / answers.size()) * 100.0) / 100.0;
    }

    private Map<String, Question> buildQuestionMap(String icafDomain) {
        List<Question> questions = questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                QUESTION_TYPE_ICAF, icafDomain);
        return questions.stream()
                .collect(java.util.stream.Collectors.toMap(Question::getQuestionKey, q -> q));
    }

    /** Normaliza score 1-5 a 0-100 */
    private double normalize(double score, String direction) {
        return "NEGATIVE".equals(direction)
                ? (5.0 - score) / 4.0 * 100.0
                : (score - 1.0) / 4.0 * 100.0;
    }

    // ── Tipos de resultado ────────────────────────────────────────────────────

    public record DomainScoreResult(
            String domain,
            double score,
            int savedCount,
            long totalAnswered
    ) {}
}
