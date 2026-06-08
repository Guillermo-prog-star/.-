package com.integrityfamily.timeline.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.timeline.dto.TimelineEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyTimelineService {

    private final EvaluationRepository evaluationRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;
    private final FamilyLogbookRepository logbookRepository;
    private final TaskEvidenceRepository evidenceRepository;
    private final CriticalDayRepository criticalDayRepository;
    private final FamilySprintRepository sprintRepository;
    private final FamilyDnaRepository dnaRepository;
    private final FamilyRepository familyRepository;

    private static final int MAX_EVENTS = 100;

    /**
     * Agrega todos los eventos familiares de todas las fuentes en un único timeline
     * ordenado cronológicamente descendente (más reciente primero).
     */
    @Transactional(readOnly = true)
    public List<TimelineEventDto> getTimeline(Long familyId) {
        List<TimelineEventDto> events = new ArrayList<>();

        events.addAll(fromEvaluations(familyId));
        events.addAll(fromGratitudes(familyId));
        events.addAll(fromLogbook(familyId));
        events.addAll(fromEvidences(familyId));
        events.addAll(fromCrises(familyId));
        events.addAll(fromMissions(familyId));
        events.addAll(fromDna(familyId));
        events.addAll(fromMemberJoins(familyId));

        return events.stream()
                .filter(e -> e.occurredAt() != null)
                .sorted(Comparator.comparing(TimelineEventDto::occurredAt).reversed())
                .limit(MAX_EVENTS)
                .collect(Collectors.toList());
    }

    // ─── Evaluaciones completadas ─────────────────────────────────────────────

    private List<TimelineEventDto> fromEvaluations(Long familyId) {
        return evaluationRepository.findByFamilyId(familyId).stream()
                .filter(e -> EvaluationStatus.FINALIZED.equals(e.getStatus()) && e.getFinalizedAt() != null)
                .map(e -> new TimelineEventDto(
                        e.getId(),
                        TimelineEventDto.EventType.EVALUATION,
                        "Diagnóstico familiar completado",
                        buildEvalDescription(e),
                        "Familia",
                        null,
                        buildEvalMetadata(e),
                        e.getFinalizedAt()
                ))
                .collect(Collectors.toList());
    }

    private String buildEvalDescription(Evaluation e) {
        if (e.getIcf() == null) return "El equipo completó su evaluación de transformación.";
        return String.format(
                "El equipo completó su diagnóstico. ICF: %.1f — Nivel de riesgo: %s.",
                e.getIcf(),
                e.getRiskLevel() != null ? e.getRiskLevel().toLowerCase() : "moderado"
        );
    }

    private String buildEvalMetadata(Evaluation e) {
        if (e.getIcf() == null) return null;
        return String.format("icf=%.1f|riesgo=%s|hito=%s",
                e.getIcf(),
                e.getRiskLevel() != null ? e.getRiskLevel() : "MODERADO",
                e.getMilestoneKey() != null ? e.getMilestoneKey() : "W1");
    }

    // ─── Gratitudes ───────────────────────────────────────────────────────────

    private List<TimelineEventDto> fromGratitudes(Long familyId) {
        return gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .map(g -> new TimelineEventDto(
                        g.getId(),
                        TimelineEventDto.EventType.GRATITUDE,
                        g.getFromMember() + " agradeció a " + g.getToMember(),
                        g.getDescription(),
                        g.getFromMember(),
                        null,
                        null,
                        g.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // ─── Bitácora ─────────────────────────────────────────────────────────────

    private List<TimelineEventDto> fromLogbook(Long familyId) {
        return logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .map(l -> new TimelineEventDto(
                        l.getId(),
                        TimelineEventDto.EventType.LOGBOOK,
                        logbookTitle(l),
                        l.getSituation(),
                        l.getCreatedBy() != null ? l.getCreatedBy() : "Familia",
                        l.getEmotionIdentified(),
                        "estado=" + (l.getStatus() != null ? l.getStatus().name().toLowerCase() : "abierto"),
                        l.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String logbookTitle(FamilyLogbookEntry l) {
        if (LogbookStatus.RESOLVED.equals(l.getStatus())) {
            return "Desafío superado y registrado en bitácora";
        }
        return "Momento de transformación registrado";
    }

    // ─── Evidencias de misiones ───────────────────────────────────────────────

    private List<TimelineEventDto> fromEvidences(Long familyId) {
        return evidenceRepository.findByFamilyId(familyId).stream()
                .filter(ev -> ev.getCreatedAt() != null)
                .map(ev -> new TimelineEventDto(
                        ev.getId(),
                        TimelineEventDto.EventType.EVIDENCE,
                        evidenceTitle(ev),
                        ev.getDescription(),
                        "Familia",
                        null,
                        "tipo=" + (ev.getEvidenceType() != null ? ev.getEvidenceType().name().toLowerCase() : "otro"),
                        ev.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private String evidenceTitle(TaskEvidence ev) {
        String tipo = ev.getEvidenceType() != null ? ev.getEvidenceType().name() : "TEXTO";
        return switch (tipo) {
            case "PHOTO"    -> "Foto subida como evidencia de misión";
            case "VIDEO"    -> "Video subido como evidencia de misión";
            case "AUDIO"    -> "Audio subido como evidencia de misión";
            case "LOCATION" -> "Ubicación registrada como evidencia";
            default         -> ev.getTitle() != null ? ev.getTitle() : "Evidencia registrada";
        };
    }

    // ─── Crisis ───────────────────────────────────────────────────────────────

    private List<TimelineEventDto> fromCrises(Long familyId) {
        return criticalDayRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .map(c -> new TimelineEventDto(
                        c.getId(),
                        TimelineEventDto.EventType.CRISIS,
                        "Crisis familiar registrada — " + (c.getCategory() != null ? c.getCategory() : ""),
                        c.getDescription(),
                        "Familia",
                        c.getEmotion(),
                        "categoria=" + c.getCategory(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // ─── Misiones completadas del sprint ─────────────────────────────────────

    private List<TimelineEventDto> fromMissions(Long familyId) {
        try {
            return sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                    .flatMap(s -> s.getMissions().stream())
                    .filter(m -> "COMPLETED".equals(m.getStatus()) && m.getCompletedAt() != null)
                    .map(m -> new TimelineEventDto(
                            m.getId(),
                            TimelineEventDto.EventType.MISSION,
                            "Misión completada",
                            m.getDescription(),
                            "Familia",
                            null,
                            null,
                            m.getCompletedAt()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[TIMELINE] Error obteniendo misiones para familia {}: {}", familyId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─── ADN Familiar ─────────────────────────────────────────────────────────

    private List<TimelineEventDto> fromDna(Long familyId) {
        return dnaRepository.findByFamilyId(familyId)
                .filter(d -> d.getUpdatedAt() != null)
                .map(d -> new TimelineEventDto(
                        d.getId(),
                        TimelineEventDto.EventType.DNA,
                        d.getVersion() > 1
                                ? "ADN Familiar actualizado (v" + d.getVersion() + ")"
                                : "ADN Familiar sintetizado por primera vez",
                        d.getNarrativaIa(),
                        "Sistema IA",
                        null,
                        "version=" + d.getVersion(),
                        d.getUpdatedAt()
                ))
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    // ─── Incorporación de miembros ────────────────────────────────────────────

    private List<TimelineEventDto> fromMemberJoins(Long familyId) {
        return familyRepository.findById(familyId)
                .map(f -> f.getMembers().stream()
                        .filter(FamilyMember::isActive)
                        .filter(m -> m.getJoinedAt() != null)
                        .map(m -> new TimelineEventDto(
                                m.getId(),
                                TimelineEventDto.EventType.MEMBER_JOINED,
                                m.getFullName() + " se unió a la familia",
                                "Un nuevo miembro comenzó su camino de transformación familiar.",
                                m.getFullName(),
                                null,
                                "rol=" + m.getRole(),
                                m.getJoinedAt()
                        ))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }
}
