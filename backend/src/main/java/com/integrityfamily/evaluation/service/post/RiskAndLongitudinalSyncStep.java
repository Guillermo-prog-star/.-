package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.risk.service.LongitudinalStateService;
import com.integrityfamily.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Snapshot de riesgo + sincronización del estado longitudinal.
 * Es el único paso que produce un dato para los siguientes: {@code riskLevel}.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class RiskAndLongitudinalSyncStep implements PostFinalizationStep {

    private final RiskService riskService;
    private final LongitudinalStateService longitudinalStateService;

    @Override
    public String name() { return "RISK_AND_LONGITUDINAL_SYNC"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        Evaluation saved = ctx.evaluation();

        RiskSnapshot snapshot = riskService.calculateAndCreate(saved.getFamily(), saved.getIcf(), saved.getHasCrisis());
        if (snapshot != null && snapshot.getRiskLevel() != null) {
            ctx.setRiskLevel(snapshot.getRiskLevel());
        }

        // ── SINCRONIZACIÓN LONGITUDINAL ───────────────────────────────────
        // Cada diagnóstico actualiza la memoria estructural de la familia.
        // El Consultor IA usará este estado longitudinal en todas sus respuestas.
        // Sin resultado del algoritmo (simulación) se sincroniza solo ICF y riesgo.
        double icf = saved.getIcf() != null ? saved.getIcf() : 50.0;
        Map<String, Double> dims = ctx.algo() != null ? ctx.algo().dimensionScores() : Map.of();
        longitudinalStateService.syncFromSnapshot(
                saved.getFamily().getId(),
                icf,
                ctx.riskLevel(),
                dims.getOrDefault("emociones",    50.0),
                dims.getOrDefault("comunicacion", 50.0),
                dims.getOrDefault("habitos",      50.0),
                dims.getOrDefault("tiempos",      50.0)
        );
        log.info("📊 [EVALUATION] Estado longitudinal sincronizado para familia {} | ICF={} | {}",
                saved.getFamily().getId(), String.format("%.1f", icf), ctx.riskLevel());
    }
}
