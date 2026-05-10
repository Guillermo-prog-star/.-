package com.integrityfamily.analytics.service;

import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.analytics.dto.DashboardSummaryResponse;
import com.integrityfamily.analytics.dto.SuggestedActionDto;
import com.integrityfamily.domain.ChecklistItem;
import com.integrityfamily.domain.repository.ChecklistItemRepository;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.RiskLevel;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.repository.FamilyLogbookRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDD: ImplementaciÃƒÂ³n del Servicio de AnalÃƒÂ­tica Proyectiva.
 * Integra IA, Riesgo e Hitos con persistencia de snapshots y sincronizaciÃƒÂ³n asÃƒÂ­ncrona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final AiService aiService;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final ChecklistItemRepository checklistRepository;
    private final FamilyLogbookRepository logbookRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public DashboardSummaryResponse calculateLatestResults(Long familyId) {
        log.info("Ã°Å¸â€œÅ  [ANALYTICS] Iniciando cÃƒÂ¡lculo integral para familia ID: {}", familyId);

        // 1. Recuperar Entidad Core
        Family family = familyRepository.findById(familyId)
                .orElseGet(() -> familyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontrÃƒÂ³ ninguna familia en el sistema.")));

        // 2. Recuperar Historial de Evaluaciones
        List<Evaluation> allEvals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId);
        Evaluation firstEval = allEvals.isEmpty() ? null : allEvals.get(0);
        Evaluation lastEval = allEvals.isEmpty() ? null : allEvals.get(allEvals.size() - 1);

        // 3. Recuperar ÃƒÅ¡ltimo Riesgo
        RiskSnapshot lastRisk = riskSnapshotRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId).orElse(null);

        // 4. CÃƒÂ¡lculo de Crecimiento de Consciencia
        double growth = 0.0;
        if (firstEval != null && firstEval.getIcf() != null && lastEval != null && lastEval.getIcf() != null) {
            growth = lastEval.getIcf() - firstEval.getIcf();
        }

        // 5. Mapeo de Dimensiones para el Radar Chart
        Map<String, Double> dims = new HashMap<>();
        if (lastEval != null && lastEval.getDimensionScores() != null) {
            lastEval.getDimensionScores().stream()
                .filter(ds -> ds != null && ds.getDimensionName() != null && ds.getScore() != null)
                .forEach(ds -> dims.put(ds.getDimensionName(), (double) ds.getScore()));
        }

        // 6. Mapeo Seguro de Nivel de Riesgo
        RiskLevel mappedLevel = RiskLevel.LOW;
        if (lastRisk != null && lastRisk.getRiskLevel() != null) {
            try {
                mappedLevel = RiskLevel.valueOf(lastRisk.getRiskLevel().toUpperCase());
            } catch (Exception e) {
                mappedLevel = RiskLevel.LOW;
            }
        }

        // 7. GeneraciÃƒÂ³n de Insight mediante IA (Motor de Pensamiento CrÃƒÂ­tico)
        String insight;
        try {
            insight = aiService.generateDashboardInsight(family, dims, mappedLevel.name());
        } catch (Exception e) {
            log.error("Ã¢Å¡Â Ã¯Â¸Â [ANALYTICS] No se pudo generar insight de IA: {}", e.getMessage());
            insight = "Sincronizando reflexiones profundas... El motor de IA estÃƒÂ¡ procesando el contexto familiar.";
        }

        // 8. RecuperaciÃƒÂ³n de Tareas del Plan de AcciÃƒÂ³n
        List<ChecklistItem> allChecklist = checklistRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        long totalItems = allChecklist.size();
        long completedItems = allChecklist.stream().filter(ChecklistItem::isCompleted).count();

        List<SuggestedActionDto> suggestedActions = allChecklist.stream()
                .limit(5)
                .map(item -> SuggestedActionDto.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .dimension(item.getDimension())
                        .completed(item.isCompleted())
                        .build())
                .collect(Collectors.toList());

        // 9. Recuperación de Bitácora (Novedad)
        long openLogbookItems = logbookRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, LogbookStatus.OPEN).size();
        String latestAgreement = logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .filter(e -> e.getFamilyAgreement() != null && !e.getFamilyAgreement().isBlank())
                .map(FamilyLogbookEntry::getFamilyAgreement)
                .findFirst()
                .orElse("No hay acuerdos recientes registrados.");

        // 10. Motor de Activación Proactiva Sentinel (Capa de Contención)
        boolean sentinelTriggered = Boolean.TRUE.equals(family.getSentinelActive())
                || (lastEval != null && lastEval.getIcf() != null && lastEval.getIcf() < 40.0)
                || (growth < -15.0)
                || (openLogbookItems > 3); // Nueva condición: más de 3 dificultades abiertas

        // 11. Ajuste de recomendación IA ante estado de crisis
        String finalInsight = sentinelTriggered ? "⚠️ [S.O.S NODO] Protocolo de Contención Activado. " + insight : insight;
        
        if (openLogbookItems > 0) {
            finalInsight += " Hay " + openLogbookItems + " situaciones pendientes en la bitácora.";
        }

        // 11. Persistencia del Snapshot (Memoria Histórica)
        RiskSnapshot snapshot = RiskSnapshot.builder()
                .family(family)
                .icf(lastEval != null && lastEval.getIcf() != null ? lastEval.getIcf() : 0.0)
                .riskLevel(mappedLevel.name())
                .hasCrisis(sentinelTriggered)
                .consciousnessLevel(lastRisk != null ? lastRisk.getConsciousnessLevel() : 1)
                .consciousnessLabel(lastRisk != null ? lastRisk.getConsciousnessLabel() : "Inconsciente")
                .createdAt(LocalDateTime.now())
                .build();
        riskSnapshotRepository.save(snapshot);

        // 12. ConstrucciÃƒÂ³n del DTO de Respuesta
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .familyId(familyId)
                .familyName(family.getName())
                .familyCode(family.getFamilyCode())
                .currentMilestone(family.getCurrentMilestone())
                .totalMembers((long) (family.getMembers() != null ? family.getMembers().size() : 0))
                .totalEvaluations((long) allEvals.size())
                .latestRiskLevel(mappedLevel)
                .latestGlobalScore(BigDecimal.valueOf(lastEval != null && lastEval.getIcf() != null ? lastEval.getIcf() : 0.0))
                .latestConsciousnessLevel(snapshot.getConsciousnessLevel())
                .latestConsciousnessLabel(snapshot.getConsciousnessLabel())
                .hasCrisis(snapshot.getHasCrisis())
                .isSentinelActive(sentinelTriggered)
                .totalChecklistItems(totalItems)
                .completedChecklistItems(completedItems)
                .totalPlanTasks(totalItems)       // SincronizaciÃƒÂ³n para el frontend
                .completedPlanTasks(completedItems) // SincronizaciÃƒÂ³n para el frontend
                .pillarProgress((double) (totalItems > 0 ? (completedItems * 100 / totalItems) : 0))
                .awarenessGrowth(growth)
                .dimensionScores(dims)
                .suggestedActions(suggestedActions)
                .aiRecommendation(finalInsight)
                .openLogbookEntriesCount(openLogbookItems)
                .latestFamilyAgreement(latestAgreement)
                .build();

        // 13. SincronizaciÃƒÂ³n AsÃƒÂ­ncrona via RabbitMQ (Disparar generaciÃƒÂ³n de planes)
        log.info("Ã°Å¸â€œÂ§ [ANALYTICS] Intentando sincronizar con RabbitMQ...");
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "tasks.suggested", response);
            log.info("Ã¢Å“â€¦ [ANALYTICS] SincronizaciÃƒÂ³n RabbitMQ completada.");
        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ [ANALYTICS] Error al enviar a RabbitMQ (No crÃƒÂ­tico para el Dashboard): {}", e.getMessage());
        }

        return response;
    }

    @Override
    public void invalidateCacheAndRecalculate(Long familyId) {
        log.warn("Ã°Å¸â€â€ž [ANALYTICS] Invalidando cachÃƒÂ© y recalculando para familia: {}", familyId);
        // En una implementaciÃƒÂ³n real con Redis, aquÃƒÂ­ se borrarÃƒÂ­a la llave.
        // Por ahora, forzamos el cÃƒÂ¡lculo directo.
        calculateLatestResults(familyId);
    }
}


