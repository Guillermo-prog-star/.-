package com.integrityfamily.trajectory.service;

import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.TrajectoryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduler diario que evalúa el estado de riesgo de cada familia y emite
 * notificaciones proactivas al guardián cuando detecta señales críticas.
 *
 * Corre a las 08:00 todos los días. No asigna trayectorias — solo notifica.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrajectoryAlertScheduler {

    private final FamilyRepository              familyRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;
    private final FamilyRiskTrajectoryRepository familyTrajRepo;
    private final TrajectorySuggestionService   suggestionService;
    private final UserNotificationService       notificationService;

    private static final List<TrajectoryStatus> ACTIVE_STATUSES =
        List.of(TrajectoryStatus.DETECTED, TrajectoryStatus.IN_PROGRESS, TrajectoryStatus.RELAPSED);

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void runDailyRiskScan() {
        List<Family> families = familyRepository.findAll();
        log.info("[TRAY-ALERT] Escaneando {} familias para alertas de riesgo", families.size());

        int alertsSent = 0;
        for (Family family : families) {
            try {
                alertsSent += evaluateFamily(family);
            } catch (Exception e) {
                log.warn("[TRAY-ALERT] Error evaluando familia {}: {}", family.getId(), e.getMessage());
            }
        }
        log.info("[TRAY-ALERT] Escaneo diario completado — {} alertas enviadas", alertsSent);
    }

    private int evaluateFamily(Family family) {
        FamilyLongitudinalState lts = ltsRepository.findByFamilyId(family.getId()).orElse(null);
        if (lts == null) return 0;

        int sent = 0;

        // 1. Alertar si el riesgo global es CRITICO
        if ("CRITICO".equalsIgnoreCase(lts.getCurrentRiskLevel())) {
            String trend = lts.getRiskTrend();
            if ("DETERIORATING".equalsIgnoreCase(trend) || "CRITICAL".equalsIgnoreCase(trend)) {
                notificationService.push(family, null, "RISK_CRITICAL_ALERT",
                    "🚨 Estado de riesgo CRÍTICO con deterioro activo",
                    "La familia presenta riesgo CRÍTICO y tendencia deteriorante. " +
                    "ICF actual: " + formatIcf(lts.getIcfCurrent()) + ". Revisión urgente recomendada.");
                sent++;
            }
        }

        // 2. Alertar si hay recaídas activas
        long relapsed = familyTrajRepo.findByFamilyId(family.getId()).stream()
            .filter(frt -> frt.getStatus() == TrajectoryStatus.RELAPSED)
            .count();
        if (relapsed > 0) {
            notificationService.push(family, null, "TRAJECTORY_RELAPSE_REMINDER",
                "⚠️ Recaída sin atender (" + relapsed + " trayectoria" + (relapsed > 1 ? "s)" : ")"),
                "Hay " + relapsed + " trayectoria(s) en estado de recaída que requieren seguimiento. " +
                "Accede a Trayectorias de Riesgo para revisar el plan de acción.");
            sent++;
        }

        // 3. Sugerir nuevas trayectorias de alto riesgo no asignadas
        Set<String> alreadyAssigned = familyTrajRepo.findByFamilyId(family.getId()).stream()
            .filter(frt -> frt.getStatus() != TrajectoryStatus.CLOSED)
            .map(frt -> frt.getTrajectory().getCode())
            .collect(Collectors.toSet());

        List<TrajectorySuggestionService.TrajectorySuggestion> suggestions =
            suggestionService.suggest(family.getId());

        List<TrajectorySuggestionService.TrajectorySuggestion> urgent = suggestions.stream()
            .filter(s -> ("CRITICAL".equals(s.severityDefault()) || "HIGH".equals(s.severityDefault()))
                      && s.confidenceScore() >= 65)
            .toList();

        if (!urgent.isEmpty()) {
            String names = urgent.stream()
                .map(TrajectorySuggestionService.TrajectorySuggestion::name)
                .limit(2)
                .collect(Collectors.joining(", "));
            notificationService.push(family, null, "TRAJECTORY_SUGGESTION_URGENT",
                "🗺️ Trayectorias de riesgo sugeridas por el sistema",
                "El análisis automático detectó señales compatibles con: " + names +
                (urgent.size() > 2 ? " y " + (urgent.size() - 2) + " más." : ".") +
                " Revisa las sugerencias en el módulo de Trayectorias.");
            sent++;
        }

        return sent;
    }

    private String formatIcf(Double icf) {
        return icf != null ? String.format("%.1f", icf) : "N/D";
    }
}
