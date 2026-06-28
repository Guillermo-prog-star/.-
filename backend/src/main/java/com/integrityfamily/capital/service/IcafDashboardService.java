package com.integrityfamily.capital.service;

import com.integrityfamily.capital.dto.IcafDashboardResponse;
import com.integrityfamily.capital.dto.IcafDashboardResponse.DomainScore;
import com.integrityfamily.capital.service.IcafScoringEngine.IcafDomains;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta los datos del dashboard ICaF para el frontend.
 * Lee el último snapshot disponible más el estado longitudinal
 * para componer la respuesta completa sin recalcular.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcafDashboardService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FamilyCapitalSnapshotRepository snapshotRepo;
    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final FamilyCriticalEventRepository criticalEventRepo;
    private final FamilyIcafAnswerRepository icafAnswerRepo;
    private final IcafScoringEngine icafScoringEngine;

    @Transactional
    public IcafDashboardResponse getDashboard(Long familyId) {

        // ── 1. Último snapshot ICaF ───────────────────────────────────────────
        Optional<FamilyCapitalSnapshot> latestOpt =
                snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(familyId);

        if (latestOpt.isEmpty()) {
            // Sin cálculo previo → calcular ahora
            log.info("[ICaF-Dashboard] Sin snapshot para familia {} — calculando", familyId);
            try {
                icafScoringEngine.compute(familyId, "SCHEDULED");
                latestOpt = snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(familyId);
            } catch (Exception e) {
                log.error("[ICaF-Dashboard] Error al calcular ICaF inicial: {}", e.getMessage());
                return emptyDashboard(familyId);
            }
        }

        if (latestOpt.isEmpty()) return emptyDashboard(familyId);

        FamilyCapitalSnapshot snap = latestOpt.get();

        // ── 2. Estado longitudinal para historial ─────────────────────────────
        Optional<FamilyLongitudinalState> stateOpt = longitudinalRepo.findByFamilyId(familyId);
        Double icaf6m  = stateOpt.map(FamilyLongitudinalState::getIcaf6mAgo).orElse(null);
        Double icaf12m = stateOpt.map(FamilyLongitudinalState::getIcaf12mAgo).orElse(null);
        Double icaf36m = stateOpt.map(FamilyLongitudinalState::getIcaf36mAgo).orElse(null);
        String trend   = stateOpt.map(FamilyLongitudinalState::getIcafTrend).orElse("STABLE");

        // ── 3. Métricas de eventos críticos ──────────────────────────────────
        long active   = criticalEventRepo.countActiveByFamilyId(familyId);
        long resolved = criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                      + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED");
        long total    = active + resolved
                      + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RELAPSED");
        double resRate   = total > 0 ? Math.round((double) resolved / total * 1000.0) / 10.0 : 100.0;
        double avgDays   = criticalEventRepo.avgDaysToResolutionByFamilyId(familyId);
        long relapses    = criticalEventRepo.totalRelapsesByFamilyId(familyId);

        // ── 4. Fuentes de datos para indicar estimaciones ─────────────────────
        boolean hasConfianza   = icafAnswerRepo.hasAnswers(familyId, IcafQuestionnaireService.DOMAIN_CONFIANZA);
        boolean hasBienestar   = icafAnswerRepo.hasAnswers(familyId, IcafQuestionnaireService.DOMAIN_BIENESTAR);
        boolean hasResiliencia = total > 0;

        // ── 5. Construir lista de dominios ────────────────────────────────────
        List<DomainScore> domains = IcafDashboardResponse.buildDomains(
                orZero(snap.getDomCohesion()),
                orZero(snap.getDomConfianza()),
                orZero(snap.getDomResiliencia()),
                orZero(snap.getDomComunicacion()),
                orZero(snap.getDomAutonomia()),
                orZero(snap.getDomBienestar()),
                orZero(snap.getDomProposito()),
                orZero(snap.getDomIntegracion()),
                orZero(snap.getDomEmprendimiento()),
                orZero(snap.getDomLegado()),
                orZero(snap.getDomMadurez()),
                hasConfianza, hasBienestar, hasResiliencia
        );

        String lastCalc = snap.getCreatedAt() != null ? snap.getCreatedAt().format(FMT) : null;

        return new IcafDashboardResponse(
                familyId,
                snap.getIcaf(),
                snap.getMadurezNivel(),
                IcafScoringEngine.madurezLabel(snap.getMadurezNivel()),
                trend,
                icaf6m, icaf12m, icaf36m,
                domains,
                active, resolved, resRate, avgDays, relapses,
                lastCalc,
                snap.getIcaf() > 0
        );
    }

    private double orZero(Double v) {
        return v != null ? v : 0.0;
    }

    private IcafDashboardResponse emptyDashboard(Long familyId) {
        return new IcafDashboardResponse(
                familyId, 0.0, 1, "Supervivencia", "STABLE",
                null, null, null, List.of(),
                0L, 0L, 100.0, 0.0, 0L,
                null, false
        );
    }
}
