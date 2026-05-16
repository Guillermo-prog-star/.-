package com.integrityfamily.analytics.messaging;

import java.util.Map;
import com.integrityfamily.analytics.domain.FamilyDashboardView;
import com.integrityfamily.analytics.repository.FamilyDashboardViewRepository;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * SDD: Sincronizador del Read Model (Dashboard).
 * Actualiza la vista desnormalizada ante cualquier evento de dominio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final FamilyDashboardViewRepository viewRepository;
    private final FamilyRepository familyRepository;

    @RabbitListener(queues = RabbitConfig.ANALYTICS_QUEUE)
    @Transactional
    public void handleSystemEvent(SystemEvent event) {
        if (event == null) {
            log.warn("⚠️ [ANALYTICS-SYNC] Recibido evento nulo");
            return;
        }
        if (event.routingKey() == null || event.familyId() == null) {
            log.warn("⚠️ [ANALYTICS-SYNC] Recibido evento incompleto o con campos nulos: {}", event);
            return;
        }

        log.info("📊 [ANALYTICS-SYNC] Actualizando Read Model para familia: {} -> Evento: {}", 
                 event.familyId(), event.routingKey());

        FamilyDashboardView view = viewRepository.findById(event.familyId())
                .orElseGet(() -> createInitialView(event.familyId()));

        switch (event.routingKey()) {
            case "evaluation.completed":
                Double icf = 0.0;
                if (event.payload() instanceof Map) {
                    Map<?, ?> payloadMap = (Map<?, ?>) event.payload();
                    Object icfVal = payloadMap.get("icf");
                    if (icfVal instanceof Number) {
                        icf = ((Number) icfVal).doubleValue();
                    }
                } else if (event.payload() instanceof Number) {
                    icf = ((Number) event.payload()).doubleValue();
                }
                view.setLatestIcf(BigDecimal.valueOf(icf));
                break;
            case "members.updated":
                // Lógica de conteo de miembros (simplificada por ahora)
                view.setTotalMembers(view.getTotalMembers() + 1);
                break;
            case "analytics.updated":
                if (event.payload() instanceof Map) {
                    Map<?, ?> payloadMap = (Map<?, ?>) event.payload();
                    view.setFamilyName(getStringValue(payloadMap, "familyName"));
                    view.setFamilyCode(getStringValue(payloadMap, "familyCode"));
                    view.setLatestIcf(getBigDecimalValue(payloadMap, "latestGlobalScore"));
                    view.setRiskLevel(getStringValue(payloadMap, "latestRiskLevel"));
                    view.setTotalMembers(getLongValue(payloadMap, "totalMembers"));
                    view.setTasksCompleted(getLongValue(payloadMap, "completedPlanTasks"));
                    view.setTasksTotal(getLongValue(payloadMap, "totalPlanTasks"));
                    view.setAdherencePercentage(getDoubleValue(payloadMap, "pillarProgress"));
                    view.setOpenCrisesCount(getLongValue(payloadMap, "openLogbookEntriesCount"));
                    view.setLatestAiRecommendation(getStringValue(payloadMap, "aiRecommendation"));
                }
                break;
            // Otros casos: plan.created, task.completed, crisis.reported...
        }

        viewRepository.save(view);
    }

    private FamilyDashboardView createInitialView(Long familyId) {
        Family f = familyRepository.findById(familyId).orElseThrow();
        return FamilyDashboardView.builder()
                .familyId(familyId)
                .familyName(f.getName())
                .familyCode(f.getFamilyCode())
                .latestIcf(BigDecimal.ZERO)
                .totalMembers(0L)
                .tasksCompleted(0L)
                .tasksTotal(0L)
                .adherencePercentage(0.0)
                .openCrisesCount(0L)
                .learningEntriesCount(0L)
                .build();
    }

    private String getStringValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Long getLongValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Map<?, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        return null;
    }
}
