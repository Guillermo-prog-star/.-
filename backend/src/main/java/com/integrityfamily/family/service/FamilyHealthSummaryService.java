package com.integrityfamily.family.service;

import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyHealthSummaryResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyHealthSummaryService {

    private final FamilyRepository          familyRepository;
    private final MemberRepository          memberRepository;
    private final EvaluationRepository      evaluationRepository;
    private final RiskSnapshotRepository    riskSnapshotRepository;
    private final FamilySprintRepository    sprintRepository;
    private final TaskEvidenceRepository    evidenceRepository;
    private final SubtleSignalRadarService  radarService;
    private final FamilyJourneyService      journeyService;

    @Transactional(readOnly = true)
    public FamilyHealthSummaryResponse summarize(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        // ── ICF ─────────────────────────────────────────────────────────────
        var evals = evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream().filter(e -> e.getStatus() == EvaluationStatus.FINALIZED
                        && e.getIcf() != null).toList();

        Double currentIcf = evals.isEmpty() ? null : evals.get(evals.size() - 1).getIcf();
        Double icfDelta30d = computeDelta30d(evals);
        String icfLabel    = FamilyHealthSummaryResponse.icfLabel(currentIcf);

        // ── Radar (sin lanzar excepción si falla) ───────────────────────────
        SubtleSignalRadarResponse radar = null;
        try {
            if (evals.size() >= 2) radar = radarService.analyze(familyId);
        } catch (Exception e) {
            log.debug("[HEALTH] Radar no disponible para familia {}: {}", familyId, e.getMessage());
        }

        String evolutionPhase = radar != null && radar.icfOverall() != null
                ? radar.icfOverall().evolutionPhase() : "inconsciente";
        String icfDirection   = radar != null && radar.icfOverall() != null
                ? radar.icfOverall().direction() : "NO_DATA";
        int highSignals = radar != null
                ? (int) radar.microSignals().stream()
                        .filter(s -> "HIGH".equals(s.severity())).count()
                : 0;

        // ── Risk snapshot ────────────────────────────────────────────────────
        var snapshots = riskSnapshotRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        String riskLevel = snapshots.isEmpty() ? "BAJO" : snapshots.get(0).getRiskLevel();

        // ── Sprint activo ────────────────────────────────────────────────────
        Optional<FamilySprint> activeSprint = sprintRepository
                .findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .findFirst();

        // ── Journey ──────────────────────────────────────────────────────────
        var journey = journeyService.evaluate(familyId);

        // ── Métricas rápidas ─────────────────────────────────────────────────
        long memberCount   = memberRepository.countByFamilyId(familyId);
        long evidenceCount = evidenceRepository.findByFamilyId(familyId).size();
        long totalSprints  = sprintRepository.countByFamilyId(familyId);

        return new FamilyHealthSummaryResponse(
                familyId, family.getName(),
                currentIcf, icfDelta30d, icfLabel, icfDirection,
                riskLevel, Boolean.TRUE.equals(family.getSentinelActive()),
                evolutionPhase, highSignals,
                journey.currentLevel(), journey.journeyProgress(), journey.nextAction(),
                activeSprint.isPresent(),
                activeSprint.map(FamilySprint::getStatus).orElse(null),
                activeSprint.map(FamilySprint::getId).orElse(null),
                memberCount, evidenceCount, totalSprints,
                LocalDateTime.now()
        );
    }

    private Double computeDelta30d(List<com.integrityfamily.domain.Evaluation> evals) {
        if (evals.size() < 2) return null;
        var current = evals.get(evals.size() - 1);
        var cutoff  = LocalDate.now().minusDays(30).atStartOfDay();
        return evals.stream()
                .filter(e -> e.getFinalizedAt() != null && e.getFinalizedAt().isBefore(cutoff))
                .max(Comparator.comparing(com.integrityfamily.domain.Evaluation::getFinalizedAt))
                .map(old -> current.getIcf() - old.getIcf())
                .orElse(null);
    }
}
