package com.integrityfamily.ai.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse;
import com.integrityfamily.simulation.service.FamilyScenarioProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fase 3 del Radar de Señales Sutiles: Narrativa Evolutiva.
 *
 * Orquesta los datos del Radar (Fase 1) y los Escenarios (Fase 2),
 * construye el prompt con PromptGenerator y llama a Claude para producir
 * el texto narrativo del tipo "La historia de esta familia está cambiando...".
 *
 * El resultado es texto Markdown listo para mostrar en el frontend.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyNarrativeService {

    private final SubtleSignalRadarService radarService;
    private final FamilyScenarioProjectionService scenarioService;
    private final PromptGenerator promptGenerator;
    private final AiProvider aiProvider;
    private final FamilyRepository familyRepository;
    private final FamilyLongitudinalStateRepository ltsRepository;

    public record NarrativeResponse(
        Long familyId,
        String familyName,
        String narrative,
        int radarConfidence,
        LocalDateTime generatedAt
    ) {}

    @Transactional(readOnly = true)
    public NarrativeResponse generate(Long familyId) {
        log.info("[NARRATIVE] Generando narrativa evolutiva para familia {}", familyId);

        Family family = familyRepository.findById(familyId)
            .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        FamilyLongitudinalState lts = ltsRepository.findByFamilyId(familyId).orElse(null);

        // Fase 1 — Radar
        SubtleSignalRadarResponse radar = radarService.analyze(familyId);

        // Fase 2 — Escenarios
        FamilyScenarioResponse scenarios = scenarioService.project(familyId);

        // Extraer datos del radar
        List<String> microSignals = extractSignalDescriptions(radar);
        List<String> strengths = extractStrengthDescriptions(radar);
        List<String> trajectories = extractTrajectoryNames(radar);

        String evolutionPhase = lts != null ? lts.getEvolutionPhase() : null;
        String narrativeStage = lts != null ? lts.getNarrativeStage() : null;
        String critDim = lts != null ? lts.getCriticalDimension() : null;

        double icfCurrent = scenarios.icfBaseline();
        Double delta30d = radar.icfOverall() != null ? radar.icfOverall().delta30d() : null;
        String icfDirection = radar.icfOverall() != null ? radar.icfOverall().direction() : "STABLE";

        // Datos de escenarios para el prompt
        double aIcf = scenarios.scenarioA().week12().icfProjected();
        String aRisk = scenarios.scenarioA().estimatedRiskLevel();
        double bIcf = scenarios.scenarioB().week12().icfProjected();
        String bRisk = scenarios.scenarioB().estimatedRiskLevel();
        double cIcf = scenarios.scenarioC().week12().icfProjected();
        String cRisk = scenarios.scenarioC().estimatedRiskLevel();

        // Construir prompt
        String prompt = promptGenerator.buildEvolutiveNarrativePrompt(
            family.getName(),
            evolutionPhase,
            narrativeStage,
            icfCurrent,
            delta30d,
            icfDirection,
            critDim,
            radar.evaluationsAnalyzed(),
            microSignals,
            strengths,
            trajectories,
            aIcf, aRisk,
            bIcf, bRisk,
            cIcf, cRisk,
            scenarios.pivotMessage(),
            scenarios.opportunityWindow()
        );

        log.debug("[NARRATIVE] Prompt construido ({} chars), invocando IA...", prompt.length());

        // Llamar a Claude con el prompt ya completo (evita doble wrapping en buildFamilyMentorPrompt)
        String narrative = aiProvider.generateWithFullPrompt(prompt);

        log.info("[NARRATIVE] Narrativa generada ({} chars)", narrative.length());

        return new NarrativeResponse(
            familyId,
            family.getName(),
            narrative,
            radar.confidenceScore(),
            LocalDateTime.now()
        );
    }

    // ─── Extracción de datos del radar ────────────────────────────────────────

    private List<String> extractSignalDescriptions(SubtleSignalRadarResponse radar) {
        if (radar.microSignals() == null) return List.of();
        return radar.microSignals().stream()
            .map(SubtleSignalRadarResponse.MicroSignal::description)
            .limit(5)
            .collect(Collectors.toList());
    }

    private List<String> extractStrengthDescriptions(SubtleSignalRadarResponse radar) {
        if (radar.strengths() == null) return List.of();
        return radar.strengths().stream()
            .map(SubtleSignalRadarResponse.InvisibleStrength::description)
            .limit(3)
            .collect(Collectors.toList());
    }

    private List<String> extractTrajectoryNames(SubtleSignalRadarResponse radar) {
        if (radar.trajectoryMatches() == null) return List.of();
        return radar.trajectoryMatches().stream()
            .filter(t -> t.confidenceScore() >= 55)
            .map(SubtleSignalRadarResponse.TrajectoryMatch::trajectoryName)
            .limit(3)
            .collect(Collectors.toList());
    }

}
