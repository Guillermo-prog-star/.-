package com.integrityfamily.trajectory.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrajectoryService {

    private final RiskTrajectoryRepository trajectoryRepo;
    private final FamilyRiskTrajectoryRepository familyTrajectoryRepo;
    private final TrajectoryTimelineEventRepository timelineRepo;
    private final TrajectoryImpactIndicatorRepository indicatorRepo;
    private final FamilyRepository familyRepo;

    // ─── Bank ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TrajectoryBankResponse getBank() {
        List<RiskTrajectory> all = trajectoryRepo.findByActiveTrue();
        Map<String, List<TrajectoryBankItem>> byMacrodomain = all.stream()
            .map(this::toItem)
            .collect(Collectors.groupingBy(t -> t.macrodomain().name()));
        return new TrajectoryBankResponse(byMacrodomain, all.size());
    }

    @Transactional(readOnly = true)
    public List<TrajectoryBankItem> getBankByMacrodomain(RiskMacrodomain macrodomain) {
        return trajectoryRepo.findByMacrodomainAndActiveTrue(macrodomain)
            .stream().map(this::toItem).toList();
    }

    // ─── Family trajectories ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FamilyTrajectoryDto> getFamilyTrajectories(Long familyId) {
        return familyTrajectoryRepo.findByFamilyId(familyId)
            .stream().map(this::toFamilyDto).toList();
    }

    @Transactional
    public FamilyTrajectoryDto assignTrajectory(Long familyId, String code, String assignedBy, String notes) {
        Family family = familyRepo.findById(familyId)
            .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));
        RiskTrajectory traj = trajectoryRepo.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Trayectoria no encontrada: " + code));

        FamilyRiskTrajectory frt = FamilyRiskTrajectory.builder()
            .family(family)
            .trajectory(traj)
            .status(TrajectoryStatus.DETECTED)
            .notes(notes)
            .assignedBy(assignedBy)
            .build();
        return toFamilyDto(familyTrajectoryRepo.save(frt));
    }

    @Transactional
    public void updateStatus(Long familyTrajectoryId, TrajectoryStatus newStatus, String notes) {
        FamilyRiskTrajectory frt = familyTrajectoryRepo.findById(familyTrajectoryId)
            .orElseThrow(() -> new IllegalArgumentException("Trayectoria familiar no encontrada: " + familyTrajectoryId));
        frt.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) frt.setNotes(notes);
        if (newStatus == TrajectoryStatus.RESOLVED || newStatus == TrajectoryStatus.CLOSED) {
            frt.setResolvedAt(LocalDateTime.now());
        }
        familyTrajectoryRepo.save(frt);
        log.info("Trayectoria {} → estado actualizado a {}", familyTrajectoryId, newStatus);
    }

    // ─── Timeline ─────────────────────────────────────────────────────────────

    @Transactional
    public TrajectoryTimelineDto addTimelineEvent(Long familyTrajectoryId, TimelineEventRequest req, String recordedBy) {
        FamilyRiskTrajectory frt = familyTrajectoryRepo.findById(familyTrajectoryId)
            .orElseThrow(() -> new IllegalArgumentException("Trayectoria familiar no encontrada: " + familyTrajectoryId));

        TrajectoryTimelineEvent event = TrajectoryTimelineEvent.builder()
            .familyTrajectory(frt)
            .eventDate(req.eventDate())
            .ageAtEvent(req.ageAtEvent())
            .eventDescription(req.eventDescription())
            .riskLevel(req.riskLevel() != null ? req.riskLevel() : "MEDIUM")
            .actionTaken(req.actionTaken())
            .result(req.result())
            .recordedBy(recordedBy)
            .build();
        return toTimelineDto(timelineRepo.save(event));
    }

    @Transactional(readOnly = true)
    public List<TrajectoryTimelineDto> getTimeline(Long familyTrajectoryId) {
        return timelineRepo.findByFamilyTrajectoryIdOrderByEventDateAsc(familyTrajectoryId)
            .stream().map(this::toTimelineDto).toList();
    }

    // ─── Impact indicators ────────────────────────────────────────────────────

    @Transactional
    public TrajectoryImpactDto upsertIndicator(Long familyTrajectoryId, IndicatorRequest req) {
        FamilyRiskTrajectory frt = familyTrajectoryRepo.findById(familyTrajectoryId)
            .orElseThrow(() -> new IllegalArgumentException("Trayectoria familiar no encontrada: " + familyTrajectoryId));

        TrajectoryImpactIndicator indicator = indicatorRepo
            .findByFamilyTrajectoryIdAndIndicatorKey(familyTrajectoryId, req.indicatorKey())
            .orElse(TrajectoryImpactIndicator.builder().familyTrajectory(frt).build());

        indicator.setIndicatorName(req.indicatorName());
        indicator.setIndicatorKey(req.indicatorKey());
        indicator.setBaselineValue(req.baselineValue());
        indicator.setCurrentValue(req.currentValue());
        indicator.setUnit(req.unit());
        indicator.setHigherIsBetter(req.higherIsBetter() != null ? req.higherIsBetter() : true);
        indicator.setNotes(req.notes());
        indicator.setMeasuredAt(LocalDateTime.now());

        return toImpactDto(indicatorRepo.save(indicator));
    }

    @Transactional(readOnly = true)
    public List<TrajectoryImpactDto> getImpactSummary(Long familyTrajectoryId) {
        return indicatorRepo.findByFamilyTrajectoryId(familyTrajectoryId)
            .stream().map(this::toImpactDto).toList();
    }

    // ─── AI context block ─────────────────────────────────────────────────────

    private static final List<TrajectoryStatus> ACTIVE_STATUSES =
        List.of(TrajectoryStatus.DETECTED, TrajectoryStatus.IN_PROGRESS, TrajectoryStatus.RELAPSED);

    @Transactional(readOnly = true)
    public String buildTrajectoryContextBlock(Long familyId) {
        List<FamilyRiskTrajectory> active = familyTrajectoryRepo.findByFamilyIdAndStatusIn(familyId, ACTIVE_STATUSES);
        if (active.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n<trayectorias_de_riesgo>\n");
        sb.append("Trayectorias de Riesgo Familiares Activas (").append(active.size()).append("):\n");

        for (FamilyRiskTrajectory frt : active) {
            RiskTrajectory t = frt.getTrajectory();
            sb.append("\n  • [").append(t.getCode()).append("] ").append(t.getName());
            sb.append(" — Estado: ").append(frt.getStatus().name());
            if (frt.getDetectedAt() != null) {
                sb.append(" desde ").append(frt.getDetectedAt().toLocalDate());
            }
            if ("CRITICAL".equals(t.getSeverityDefault()) || "HIGH".equals(t.getSeverityDefault())) {
                sb.append(" ⚠️");
            }
            if (frt.getNotes() != null && !frt.getNotes().isBlank()) {
                sb.append("\n    Notas: ").append(frt.getNotes());
            }

            // Last timeline event
            List<TrajectoryTimelineEvent> events = timelineRepo
                .findByFamilyTrajectoryIdOrderByEventDateAsc(frt.getId());
            if (!events.isEmpty()) {
                TrajectoryTimelineEvent last = events.get(events.size() - 1);
                sb.append("\n    Último evento (").append(last.getEventDate()).append("): ")
                  .append(last.getEventDescription());
                if (last.getResult() != null && !last.getResult().isBlank()) {
                    sb.append(" → ").append(last.getResult());
                }
            }

            // Key indicators
            List<TrajectoryImpactIndicator> indicators = indicatorRepo.findByFamilyTrajectoryId(frt.getId());
            if (!indicators.isEmpty()) {
                sb.append("\n    Indicadores: ");
                indicators.forEach(ind -> {
                    sb.append(ind.getIndicatorName()).append("=").append(ind.getCurrentValue());
                    if (ind.getUnit() != null) sb.append(ind.getUnit());
                    sb.append(" ");
                });
            }
        }
        sb.append("\n</trayectorias_de_riesgo>\n");
        return sb.toString();
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private TrajectoryBankItem toItem(RiskTrajectory t) {
        return new TrajectoryBankItem(t.getId(), t.getCode(), t.getName(), t.getMacrodomain(),
            t.getDescription(), t.getEarlySignals(), t.getPotentialEvolution(), t.getSeverityDefault());
    }

    private FamilyTrajectoryDto toFamilyDto(FamilyRiskTrajectory frt) {
        return new FamilyTrajectoryDto(frt.getId(), toItem(frt.getTrajectory()),
            frt.getStatus(), frt.getDetectedAt(), frt.getResolvedAt(),
            frt.getNotes(), frt.getAssignedBy());
    }

    private TrajectoryTimelineDto toTimelineDto(TrajectoryTimelineEvent e) {
        return new TrajectoryTimelineDto(e.getId(), e.getEventDate(), e.getAgeAtEvent(),
            e.getEventDescription(), e.getRiskLevel(), e.getActionTaken(),
            e.getResult(), e.getRecordedAt());
    }

    private TrajectoryImpactDto toImpactDto(TrajectoryImpactIndicator i) {
        Double pct = null;
        if (i.getBaselineValue() != null && i.getCurrentValue() != null
                && i.getBaselineValue().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal delta = i.getCurrentValue().subtract(i.getBaselineValue());
            pct = delta.divide(i.getBaselineValue().abs(), 4, RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100)).doubleValue();
            if (Boolean.FALSE.equals(i.getHigherIsBetter())) pct = -pct;
        }
        return new TrajectoryImpactDto(i.getId(), i.getIndicatorName(), i.getIndicatorKey(),
            i.getBaselineValue(), i.getCurrentValue(), i.getUnit(), i.getHigherIsBetter(), pct);
    }
}
