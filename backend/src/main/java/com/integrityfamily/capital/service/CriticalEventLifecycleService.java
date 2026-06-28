package com.integrityfamily.capital.service;

import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.event.FamilyCrisisEvent;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyCriticalEvent;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona el ciclo de vida de FamilyCriticalEvent como fuente de datos
 * del dominio resiliencia en el ICaF.
 *
 * Responsabilidades:
 *   1. Crear un FamilyCriticalEvent al recibir FamilyCrisisEvent del bus existente.
 *   2. Registrar inicio de intervención, resolución y recaídas.
 *   3. Disparar recálculo ICaF tras resolución (trigger CRITICAL_EVENT).
 *   4. Proveer métricas de resiliencia a IcafResilienciaEngine.
 */
@Slf4j
@Service
public class CriticalEventLifecycleService {

    private final FamilyCriticalEventRepository criticalEventRepo;
    private final FamilyCapitalSnapshotRepository capitalSnapshotRepo;
    private final FamilyRepository familyRepo;
    private final EventPublisher eventPublisher;

    // @Lazy rompe el ciclo: CriticalEventLifecycleService → IcafScoringEngine
    //   → IcafDomainResolver → IcafResilienciaEngine → FamilyCriticalEventRepository
    //   → (misma bean) CriticalEventLifecycleService
    private final IcafScoringEngine icafScoringEngine;

    @Autowired
    public CriticalEventLifecycleService(
            FamilyCriticalEventRepository criticalEventRepo,
            FamilyCapitalSnapshotRepository capitalSnapshotRepo,
            FamilyRepository familyRepo,
            EventPublisher eventPublisher,
            @Lazy IcafScoringEngine icafScoringEngine) {
        this.criticalEventRepo    = criticalEventRepo;
        this.capitalSnapshotRepo  = capitalSnapshotRepo;
        this.familyRepo           = familyRepo;
        this.eventPublisher       = eventPublisher;
        this.icafScoringEngine    = icafScoringEngine;
    }

    // ── Listener del bus existente ────────────────────────────────────────────

    /**
     * Escucha FamilyCrisisEvent (publicado por CrisisServiceImpl) y crea
     * el FamilyCriticalEvent correspondiente si no existe aún para ese criticalDayId.
     * Idempotente: si ya existe para ese criticalDayId, no duplica.
     */
    @Async
    @EventListener
    @Transactional
    public void onCrisisTriggered(FamilyCrisisEvent event) {
        Long familyId = event.familyId();

        // Evitar duplicados si el evento se procesa más de una vez
        if (event.criticalDayId() != null) {
            Optional<FamilyCriticalEvent> existing =
                    criticalEventRepo.findByFamilyIdAndCriticalDayId(familyId, event.criticalDayId());
            if (existing.isPresent()) {
                log.debug("[ICaF-Crisis] FamilyCriticalEvent ya existe para criticalDayId={}", event.criticalDayId());
                return;
            }
        }

        Family family = familyRepo.findById(familyId).orElse(null);
        if (family == null) {
            log.warn("[ICaF-Crisis] Familia {} no encontrada — FamilyCriticalEvent no creado", familyId);
            return;
        }

        // ICaF actual al momento de la detección
        Double currentIcaf = capitalSnapshotRepo
                .findTopByFamilyIdOrderByCreatedAtDesc(familyId)
                .map(s -> s.getIcaf())
                .orElse(null);

        FamilyCriticalEvent criticalEvent = FamilyCriticalEvent.builder()
                .family(family)
                .category(normalizeCategoryFromCrisis(event.category()))
                .status("DETECTED")
                .severity(inferSeverity(event))
                .detectedAt(LocalDate.now())
                .icafAtDetection(currentIcaf)
                .criticalDayId(event.criticalDayId())
                .notes(event.description())
                .build();

        criticalEventRepo.save(criticalEvent);
        log.info("[ICaF-Crisis] FamilyCriticalEvent creado | familia={} | categoría={} | icafAtDetection={}",
                familyId, criticalEvent.getCategory(),
                currentIcaf != null ? String.format("%.1f", currentIcaf) : "N/A");
    }

    // ── API de gestión del ciclo de vida ──────────────────────────────────────

    /** Marca el inicio formal de la intervención */
    @Transactional
    public FamilyCriticalEvent startIntervention(Long eventId) {
        FamilyCriticalEvent event = findOrThrow(eventId);
        event.setStatus("IN_PROGRESS");
        event.setInterventionStartAt(LocalDate.now());
        FamilyCriticalEvent saved = criticalEventRepo.save(event);
        log.info("[ICaF-Crisis] Intervención iniciada | eventId={} | familia={}",
                eventId, event.getFamily().getId());
        return saved;
    }

    /**
     * Marca el evento como resuelto.
     * Dispara recálculo ICaF con trigger CRITICAL_EVENT para actualizar resiliencia.
     */
    @Transactional
    public FamilyCriticalEvent resolve(Long eventId, String resolutionSummary) {
        FamilyCriticalEvent event = findOrThrow(eventId);
        Long familyId = event.getFamily().getId();

        // ICaF actual al resolver
        Double currentIcaf = capitalSnapshotRepo
                .findTopByFamilyIdOrderByCreatedAtDesc(familyId)
                .map(s -> s.getIcaf())
                .orElse(null);

        event.markResolved(resolutionSummary, currentIcaf);
        criticalEventRepo.save(event);

        log.info("[ICaF-Crisis] Evento resuelto | eventId={} | familia={} | diasResolucion={} | recaidas={}",
                eventId, familyId, event.getDaysToResolution(), event.getRelapseCount());

        // Disparar recálculo ICaF: la resiliencia mejora con cada resolución
        triggerIcafRecalculation(familyId);

        return event;
    }

    /** Registra una recaída en un evento previamente resuelto */
    @Transactional
    public FamilyCriticalEvent registerRelapse(Long eventId, String notes) {
        FamilyCriticalEvent event = findOrThrow(eventId);
        event.registerRelapse();
        if (notes != null) event.setNotes(notes);
        FamilyCriticalEvent saved = criticalEventRepo.save(event);

        log.warn("[ICaF-Crisis] RECAÍDA registrada | eventId={} | familia={} | recaida#{}",
                eventId, event.getFamily().getId(), saved.getRelapseCount());

        // Recálculo ICaF: la recaída impacta resiliencia
        triggerIcafRecalculation(event.getFamily().getId());

        return saved;
    }

    /** Cierra definitivamente un evento (sin más seguimiento) */
    @Transactional
    public FamilyCriticalEvent close(Long eventId) {
        FamilyCriticalEvent event = findOrThrow(eventId);
        event.setStatus("CLOSED");
        event.setClosedAt(LocalDate.now());
        return criticalEventRepo.save(event);
    }

    // ── Consultas de resiliencia ──────────────────────────────────────────────

    public List<FamilyCriticalEvent> getActiveEvents(Long familyId) {
        return criticalEventRepo.findActiveByFamilyId(familyId);
    }

    public List<FamilyCriticalEvent> getResolvedEvents(Long familyId) {
        return criticalEventRepo.findResolvedByFamilyId(familyId);
    }

    public ResilienceMetrics getResilienceMetrics(Long familyId) {
        long totalEvents   = criticalEventRepo.countByFamilyIdAndStatus(familyId, "DETECTED")
                           + criticalEventRepo.countByFamilyIdAndStatus(familyId, "IN_PROGRESS")
                           + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                           + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED")
                           + criticalEventRepo.countByFamilyIdAndStatus(familyId, "RELAPSED");
        long resolved      = criticalEventRepo.countByFamilyIdAndStatus(familyId, "RESOLVED")
                           + criticalEventRepo.countByFamilyIdAndStatus(familyId, "CLOSED");
        long active        = criticalEventRepo.countActiveByFamilyId(familyId);
        double avgDays     = criticalEventRepo.avgDaysToResolutionByFamilyId(familyId);
        long relapses      = criticalEventRepo.totalRelapsesByFamilyId(familyId);

        return new ResilienceMetrics(totalEvents, resolved, active, avgDays, relapses);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void triggerIcafRecalculation(Long familyId) {
        try {
            icafScoringEngine.compute(familyId, "CRITICAL_EVENT");
        } catch (Exception e) {
            log.error("[ICaF-Crisis] Error en recálculo ICaF post-evento: {}", e.getMessage());
        }
    }

    private FamilyCriticalEvent findOrThrow(Long eventId) {
        return criticalEventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("FamilyCriticalEvent no encontrado: " + eventId));
    }

    private String normalizeCategoryFromCrisis(String category) {
        if (category == null) return "OTRO";
        return switch (category.toUpperCase()) {
            case "ALCOHOL", "DROGAS", "SUSTANCIAS" -> "ALCOHOL";
            case "VIOLENCIA", "AGRESION"            -> "VIOLENCIA";
            case "COMUNICACION", "CONFLICTO"        -> "COMUNICACION_ROTA";
            case "ADOLESCENTE", "HIJO"              -> "ADOLESCENTE_AISLADO";
            default                                  -> category.toUpperCase();
        };
    }

    private String inferSeverity(FamilyCrisisEvent event) {
        // La emoción puede indicar la severidad relativa
        if (event.emotion() == null) return "MODERATE";
        String em = event.emotion().toLowerCase();
        if (em.contains("terror") || em.contains("pánico") || em.contains("desesper")) return "CRITICAL";
        if (em.contains("miedo") || em.contains("angustia") || em.contains("agres"))   return "HIGH";
        if (em.contains("tristeza") || em.contains("enojo") || em.contains("frustrac")) return "MODERATE";
        return "LOW";
    }

    // ── Tipos de resultado ────────────────────────────────────────────────────

    public record ResilienceMetrics(
            long totalEvents,
            long resolvedEvents,
            long activeEvents,
            double avgDaysToResolution,
            long totalRelapses
    ) {
        /** Tasa de resolución 0-100 */
        public double resolutionRate() {
            if (totalEvents == 0) return 100.0;
            return Math.round((double) resolvedEvents / totalEvents * 100.0 * 10.0) / 10.0;
        }
    }
}
