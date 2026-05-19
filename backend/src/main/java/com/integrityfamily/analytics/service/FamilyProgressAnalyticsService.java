package com.integrityfamily.analytics.service;

import com.integrityfamily.analytics.domain.ProgressSnapshot;
import com.integrityfamily.analytics.dto.FamilyProgressResponse;
import com.integrityfamily.analytics.repository.ProgressSnapshotRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationDimensionScore;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyProgressAnalyticsService {

    private final EvaluationRepository evaluationRepository;
    private final ProgressSnapshotRepository progressSnapshotRepository;

    @Transactional
    public FamilyProgressResponse analyzeProgress(Long currentEvaluationId) {
        log.info("📊 [ANALYTICS] Analizando progreso para la evaluación ID: {}", currentEvaluationId);
        
        Evaluation current = evaluationRepository.findById(currentEvaluationId)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada: " + currentEvaluationId));

        if (current.getStatus() != EvaluationStatus.FINALIZED) {
            throw new RuntimeException("La evaluación actual no está finalizada.");
        }

        Long familyId = current.getFamily().getId();

        // Buscar todas las evaluaciones finalizadas de la familia ordenadas por fecha ascendente
        List<Evaluation> evaluations = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId);
        
        // Filtrar solo las finalizadas
        List<Evaluation> finalizedEvaluations = evaluations.stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED)
                .toList();

        if (finalizedEvaluations.size() <= 1) {
            log.info("ℹ️ No hay evaluación anterior para comparar. Es la primera evaluación.");
            
            ProgressSnapshot snapshot = ProgressSnapshot.builder()
                    .family(current.getFamily())
                    .currentEvaluation(current)
                    .milestoneCode(current.getMilestoneKey())
                    .currentIcf(current.getIcf())
                    .classification("INICIAL")
                    .interpretation("Esta es la primera evaluación de la familia. Se establece la línea base.")
                    .dimensionEvolution(new HashMap<>())
                    .recommendedAction("Completar el plan asignado para ver progreso en la siguiente evaluación.")
                    .createdAt(LocalDateTime.now())
                    .build();
            
            progressSnapshotRepository.save(snapshot);
            
            return FamilyProgressResponse.builder()
                    .familyId(familyId)
                    .currentEvaluationId(currentEvaluationId)
                    .milestoneCode(current.getMilestoneKey())
                    .currentIcf(current.getIcf())
                    .classification("INICIAL")
                    .interpretation("Esta es la primera evaluación de la familia. Se establece la línea base.")
                    .dimensionEvolution(new HashMap<>())
                    .recommendedAction("Completar el plan asignado para ver progreso en la siguiente evaluación.")
                    .build();
        }

        // Encontrar la evaluación anterior
        Evaluation previous = null;
        for (int i = 0; i < finalizedEvaluations.size(); i++) {
            if (finalizedEvaluations.get(i).getId().equals(currentEvaluationId)) {
                if (i > 0) {
                    previous = finalizedEvaluations.get(i - 1);
                }
                break;
            }
        }

        if (previous == null) {
            throw new RuntimeException("No se pudo determinar la evaluación anterior.");
        }

        Double currentIcf = current.getIcf() != null ? current.getIcf() : 0.0;
        Double previousIcf = previous.getIcf() != null ? previous.getIcf() : 0.0;
        Double deltaIcf = currentIcf - previousIcf;

        // Clasificación (Rediseño 6.6)
        String classification;
        String interpretation;
        String recommendedAction;

        if (deltaIcf >= 10) {
            classification = "MEJORA_FUERTE";
            interpretation = "La familia muestra una mejora fuerte y consolidada en su conciencia familiar.";
            recommendedAction = "Mantener plan y reforzar hábitos exitosos.";
        } else if (deltaIcf >= 3) {
            classification = "MEJORA_LEVE";
            interpretation = "La familia muestra una mejora leve en su conciencia familiar, con avances sostenidos pero aún no consolidados.";
            recommendedAction = "Continuar con ajustes menores.";
        } else if (deltaIcf >= -2) {
            classification = "ESTANCAMIENTO";
            interpretation = "La familia se encuentra en una meseta o estancamiento. No hay retroceso pero tampoco avance significativo.";
            recommendedAction = "Revisar adherencia, evidencias y tareas vencidas.";
        } else {
            classification = "DETERIORO";
            interpretation = "Se detecta un deterioro en el Índice de Conciencia Familiar. Se requiere atención prioritaria.";
            recommendedAction = "Activar alerta y rediseñar microacciones.";
        }

        // Evolución por dimensiones
        Map<String, Double> dimensionEvolution = new HashMap<>();
        Map<String, Double> currentScores = getDimensionScores(current);
        Map<String, Double> previousScores = getDimensionScores(previous);

        for (String dim : currentScores.keySet()) {
            Double currScore = currentScores.get(dim);
            Double prevScore = previousScores.getOrDefault(dim, 0.0);
            dimensionEvolution.put(dim, currScore - prevScore);
        }

        // Persistir el resultado (Rediseño 6.6)
        ProgressSnapshot snapshot = ProgressSnapshot.builder()
                .family(current.getFamily())
                .currentEvaluation(current)
                .previousEvaluation(previous)
                .milestoneCode(current.getMilestoneKey())
                .previousIcf(previousIcf)
                .currentIcf(currentIcf)
                .deltaIcf(deltaIcf)
                .classification(classification)
                .interpretation(interpretation)
                .dimensionEvolution(dimensionEvolution)
                .recommendedAction(recommendedAction)
                .createdAt(LocalDateTime.now())
                .build();

        progressSnapshotRepository.save(snapshot);
        log.info("✅ [ANALYTICS] ProgressSnapshot guardado con éxito.");

        return FamilyProgressResponse.builder()
                .familyId(familyId)
                .currentEvaluationId(currentEvaluationId)
                .previousEvaluationId(previous != null ? previous.getId() : null)
                .milestoneCode(current.getMilestoneKey())
                .previousIcf(previousIcf)
                .currentIcf(currentIcf)
                .deltaIcf(deltaIcf)
                .classification(classification)
                .interpretation(interpretation)
                .dimensionEvolution(dimensionEvolution)
                .recommendedAction(recommendedAction)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<FamilyProgressResponse> getLatestProgress(Long familyId) {
        log.info("📊 [ANALYTICS] Obteniendo último snapshot de progreso para familia: {}", familyId);
        return progressSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId)
                .map(this::mapToResponse);
    }

    private FamilyProgressResponse mapToResponse(ProgressSnapshot snapshot) {
        return FamilyProgressResponse.builder()
                .familyId(snapshot.getFamily().getId())
                .currentEvaluationId(snapshot.getCurrentEvaluation().getId())
                .previousEvaluationId(snapshot.getPreviousEvaluation() != null ? snapshot.getPreviousEvaluation().getId() : null)
                .milestoneCode(snapshot.getMilestoneCode())
                .previousIcf(snapshot.getPreviousIcf())
                .currentIcf(snapshot.getCurrentIcf())
                .deltaIcf(snapshot.getDeltaIcf())
                .classification(snapshot.getClassification())
                .interpretation(snapshot.getInterpretation())
                .dimensionEvolution(snapshot.getDimensionEvolution())
                .recommendedAction(snapshot.getRecommendedAction())
                .build();
    }

    private Map<String, Double> getDimensionScores(Evaluation evaluation) {
        Map<String, Double> scores = new HashMap<>();
        if (evaluation.getDimensionScores() != null) {
            for (EvaluationDimensionScore ds : evaluation.getDimensionScores()) {
                scores.put(ds.getDimensionName(), ds.getScore());
            }
        }
        return scores;
    }
}
