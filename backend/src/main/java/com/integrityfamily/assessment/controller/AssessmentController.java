package com.integrityfamily.assessment.controller;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.assessment.service.AssessmentAnswerService;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "2. Assessment & Diagnóstico", description = "Contrato de servicios para el diagnóstico psicométrico familiar, aplicación del algoritmo RISK_ALGO_V1 y consulta del timeline evolutivo.")
public class AssessmentController {

    private final EvaluationService evaluationService;
    private final AssessmentAnswerService assessmentAnswerService;
    private final QuestionRepository questionRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;

    /**
     * Mapeo de hitos legado → nuevo formato.
     * Familias que aún tienen el valor "MES_00_DIAGNOSTICO" arrancan en W1.
     */
    private static final Map<String, String> MILESTONE_ALIAS = Map.of(
        "MES_00_DIAGNOSTICO", "W1",
        "M00", "W1",
        "MES_01", "M1",
        "MES_03", "M3",
        "MES_06", "M6",
        "MES_12", "M12"
    );

    @Operation(summary = "Obtener set psicométrico adaptativo (Taxonomía v2)",
               description = "Selecciona 20 preguntas calibradas por hito: "
                           + "6 CORE, 6 ADAPTIVE por dimensión vulnerable, "
                           + "4 FASE_PILLAR, 2 MIRROR de control, 2 EXPLORATORIAS. "
                           + "Si se pasa `milestoneCode` explícito, tiene prioridad sobre el hito "
                           + "almacenado en la familia (útil para pre-carga y herramientas de admin).")
    @PreAuthorize("#familyId == null or @familySecurity.check(#familyId)")
    @GetMapping("/random")
    public ApiResponse<List<Question>> getRandomQuestions(
            @Parameter(description = "ID de la familia — adapta reactivos al riesgo y dimensión crítica detectada", required = false)
            @RequestParam(required = false) Long familyId,
            @Parameter(description = "Hito explícito W1–M36. Si se omite, se usa el hito actual de la familia.", required = false)
            @RequestParam(required = false) String milestoneCode) {

        log.info("[ASSESSMENT-V2] Solicitud de set adaptativo. familyId={} | milestoneCode={}", familyId, milestoneCode);

        // ── Resolver el hito efectivo ────────────────────────────────────────
        // Prioridad: milestoneCode explícito > hito de la familia > W1 por defecto
        String currentMilestone;
        String vulnerableDimension = "comunicacion"; // default si no hay historial

        if (milestoneCode != null && !milestoneCode.isBlank()) {
            // Hito explícito — normalizar alias legado y usar directamente
            currentMilestone = MILESTONE_ALIAS.getOrDefault(milestoneCode.trim(), milestoneCode.trim());
            if (familyId != null) {
                vulnerableDimension = detectVulnerableDimension(familyId);
            }
            log.info("[ASSESSMENT-V2] Hito explícito: {} | Dim. crítica: {}", currentMilestone, vulnerableDimension);

        } else if (familyId != null) {
            // Sin milestoneCode — leer desde la familia
            Optional<Family> familyOpt = familyRepository.findById(familyId);
            if (familyOpt.isEmpty()) {
                return ApiResponse.ok(getDefaultFallbackQuestions(20));
            }
            Family family = familyOpt.get();
            String raw = family.getCurrentMilestone() != null ? family.getCurrentMilestone() : "W1";
            currentMilestone = MILESTONE_ALIAS.getOrDefault(raw, raw);
            vulnerableDimension = detectVulnerableDimension(familyId);
            log.info("[ASSESSMENT-V2] Familia: {} | Hito: {} | Dim. crítica: {}",
                    family.getName(), currentMilestone, vulnerableDimension);

        } else {
            // Sin ninguno de los dos parámetros — set de diagnóstico inicial W1
            return ApiResponse.ok(getDefaultFallbackQuestions(20));
        }

        Set<Question> selected = new LinkedHashSet<>();

        // --- Pool 1: CORE del hito actual (6) ---
        List<Question> corePool = new ArrayList<>(
            questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(currentMilestone, "CORE"));
        Collections.shuffle(corePool);
        drawQuestions(selected, corePool, 6);

        // --- Pool 2: ADAPTIVE de la dimensión vulnerable (6) ---
        List<Question> adaptivePool = new ArrayList<>(
            questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(currentMilestone, "ADAPTIVE"));
        // Priorizar los de la dimensión crítica detectada
        // (captura final requerida por la lambda — vulnerableDimension puede reasignarse arriba)
        final String criticalDim = vulnerableDimension;
        adaptivePool.sort(Comparator.comparingInt(q ->
            criticalDim.equalsIgnoreCase(q.getDimension()) ? 0 : 1));
        drawQuestions(selected, adaptivePool, 6);

        // --- Pool 3: FASE_PILLAR del hito (4) ---
        List<Question> phasePool = new ArrayList<>(
            questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(currentMilestone, "FASE_PILLAR"));
        Collections.shuffle(phasePool);
        drawQuestions(selected, phasePool, 4);

        // --- Pool 4: MIRROR — siempre globales para detectar simulación (2) ---
        List<Question> mirrorPool = new ArrayList<>(
            questionRepository.findByTypeAndActiveTrue("MIRROR"));
        Collections.shuffle(mirrorPool);
        drawQuestions(selected, mirrorPool, 2);

        // --- Pool 5: EXPLORATORY del hito (2) ---
        List<Question> exploPool = new ArrayList<>(
            questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(currentMilestone, "EXPLORATORY"));
        Collections.shuffle(exploPool);
        drawQuestions(selected, exploPool, 2);

        // --- Relleno si no se alcanzaron 20 (hito con pocas preguntas) ---
        if (selected.size() < 20) {
            List<Question> fallback = new ArrayList<>(
                questionRepository.findByMilestoneCodeAndActiveTrue(currentMilestone));
            Collections.shuffle(fallback);
            for (Question q : fallback) {
                if (selected.size() >= 20) break;
                selected.add(q);
            }
        }

        // Mezclar para evitar que el usuario identifique el patrón CORE/ADAPTIVE/...
        List<Question> finalSet = new ArrayList<>(selected);
        Collections.shuffle(finalSet);

        log.info("[ASSESSMENT-V2] Set final: {} preguntas para familia {} en hito {}",
                finalSet.size(), familyId, currentMilestone);
        return ApiResponse.ok(finalSet);
    }

    private String detectVulnerableDimension(Long familyId) {
        Optional<Evaluation> lastEvalOpt = evaluationRepository
            .findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(familyId, EvaluationStatus.FINALIZED);
        if (lastEvalOpt.isEmpty()) return "comunicacion";

        Evaluation lastEval = lastEvalOpt.get();
        if (lastEval.getDimensionScores() == null || lastEval.getDimensionScores().isEmpty())
            return "comunicacion";

        return lastEval.getDimensionScores().stream()
                .min(Comparator.comparingDouble(EvaluationDimensionScore::getScore))
                .map(EvaluationDimensionScore::getDimensionName)
                .orElse("comunicacion");
    }

    private void drawQuestions(Set<Question> target, List<Question> source, int limit) {
        int drawn = 0;
        for (Question q : source) {
            if (drawn >= limit) break;
            if (target.add(q)) drawn++;
        }
    }

    private List<Question> getDefaultFallbackQuestions(int limit) {
        List<Question> list = questionRepository.findByMilestoneCodeAndActiveTrue("W1");
        if (list.size() < limit) list = questionRepository.findByActiveTrue();
        Collections.shuffle(list);
        return list.stream().limit(limit).toList();
    }

    @Operation(summary = "Obtener historial de evaluaciones de la familia", description = "Devuelve todas las sesiones de evaluación asociadas a un núcleo familiar.")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/history")
    public ResponseEntity<ApiResponse<List<EvaluationDtos.EvaluationResponse>>> getHistory(@PathVariable Long familyId) {
        List<EvaluationDtos.EvaluationResponse> history = evaluationService.findSummaryByFamilyId(familyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    private EvaluationDtos.EvaluationResponse mapToResponse(com.integrityfamily.domain.repository.EvaluationSummary evaluation) {
        return new EvaluationDtos.EvaluationResponse(
            evaluation.getId(),
            evaluation.getFamilyId(),
            evaluation.getMemberId(),
            evaluation.getStatus(),
            evaluation.getStartedAt(),
            evaluation.getFinalizedAt(),
            evaluation.getIcf(),
            evaluation.getRiskLevel(),
            evaluation.getCriticalDimension()
        );
    }

    @Operation(summary = "Consultar timeline de evolución diagnóstica", description = "Devuelve el historial evolutivo con el cálculo de índice saludable, nivel de riesgo y dimensión crítica por fecha.")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/family/{familyId}/timeline")
    public ResponseEntity<ApiResponse<List<EvaluationDtos.TimelineEntryDto>>> getTimeline(
            @Parameter(description = "ID del núcleo familiar", required = true) @PathVariable Long familyId) {
        return ResponseEntity.ok(ApiResponse.ok(evaluationService.getTimeline(familyId)));
    }

    // ── Flujo incremental ─────────────────────────────────────────────────────

    @Operation(
        summary = "Guardar respuestas durante el cuestionario (mobile-first)",
        description = "Acepta 1 o más respuestas y las persiste de forma idempotente (upsert). "
                    + "Si la app se cierra, el usuario puede retomar donde se quedó. "
                    + "Retorna el progreso actualizado: cuántas respondidas y si ya puede finalizar.")
    @PreAuthorize("@familySecurity.checkEvaluation(#evalId)")
    @PostMapping("/{evalId}/answers")
    public ResponseEntity<ApiResponse<EvaluationDtos.AnswerProgressResponse>> saveAnswers(
            @PathVariable Long evalId,
            @RequestBody @Valid List<EvaluationDtos.SaveAnswerRequest> answers) {

        log.info("[ASSESSMENT] Guardando {} respuesta(s) incremental(es) para evaluación {}",
                answers.size(), evalId);
        EvaluationDtos.AnswerProgressResponse progress =
                assessmentAnswerService.saveAnswers(evalId, answers);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    @Operation(
        summary = "Consultar progreso del cuestionario",
        description = "Devuelve cuántas preguntas fueron respondidas, la lista para reanudar "
                    + "y si ya se cumplen los mínimos para finalizar la evaluación.")
    @PreAuthorize("@familySecurity.checkEvaluation(#evalId)")
    @GetMapping("/{evalId}/answers")
    public ResponseEntity<ApiResponse<EvaluationDtos.AnswerProgressResponse>> getAnswerProgress(
            @PathVariable Long evalId) {

        log.info("[ASSESSMENT] Consultando progreso de evaluación {}", evalId);
        EvaluationDtos.AnswerProgressResponse progress =
                assessmentAnswerService.getProgress(evalId);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Iniciar nueva sesión de evaluación", description = "Crea una nueva instancia de evaluación en estado STARTED.")
    @PreAuthorize("@familySecurity.check(#req.familyId)")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<EvaluationDtos.EvaluationResponse>> startEvaluation(@RequestBody EvaluationDtos.EvaluationStartRequest req) {
        Evaluation evaluation = evaluationService.start(req);
        return ResponseEntity.ok(ApiResponse.ok(mapToResponse(evaluation)));
    }

    @Operation(summary = "Finalizar evaluación y aplicar RISK_ALGO_V1 (Taxonomía v2)",
               description = "Recibe las respuestas, aplica RISK_ALGO_V1 con severityWeight y detección "
                           + "de simulación MIRROR. Retorna ICF, riesgo, scores por dimensión, "
                           + "nivel de consciencia, misión sugerida y alertas de recaída/simulación.")
    @PreAuthorize("@familySecurity.checkEvaluation(#id)")
    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<EvaluationDtos.EvaluationResultResponse>> finalizeEvaluation(
            @PathVariable Long id,
            @RequestBody EvaluationDtos.EvaluationFinalizeRequest req) {
        EvaluationDtos.FinalizeResult result = evaluationService.finalize(id, req);
        return ResponseEntity.ok(ApiResponse.ok(result.algoResult()));
    }

    @GetMapping("/resilience-check")
    public ResponseEntity<String> getResilienceCheck() {
        return ResponseEntity.ok("RESILIENCE_PATCH_ACTIVE_2026_05_24_V1");
    }

    private EvaluationDtos.EvaluationResponse mapToResponse(Evaluation evaluation) {
        return new EvaluationDtos.EvaluationResponse(
            evaluation.getId(),
            evaluation.getFamily().getId(),
            evaluation.getMember() != null ? evaluation.getMember().getId() : null,
            evaluation.getStatus(),
            evaluation.getStartedAt(),
            evaluation.getFinalizedAt(),
            evaluation.getIcf(),
            evaluation.getRiskLevel(),
            evaluation.getCriticalDimension()
        );
    }
}
