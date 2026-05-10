package com.integrityfamily.family.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyBehavioralEvent;
import com.integrityfamily.domain.repository.FamilyBehavioralEventRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.dto.BehavioralEventResponse;
import com.integrityfamily.dto.CreateBehavioralEventRequest;
import com.integrityfamily.dto.FamilyIvrSummary;
import com.integrityfamily.dto.RepairBehavioralEventRequest;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyBehavioralEventService {

    private final FamilyRepository familyRepository;
    private final FamilyBehavioralEventRepository repository;

    /**
     * Registra un nuevo evento conductual de fricción.
     */
    @Transactional
    public BehavioralEventResponse create(CreateBehavioralEventRequest request) {
        log.info("[IVR] Registrando nueva fricción familiar para la familia ID: {}, severidad: {}", 
                request.familyId(), request.severity());

        Family family = familyRepository.findById(request.familyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + request.familyId(), "NOT_FOUND", HttpStatus.NOT_FOUND));

        FamilyBehavioralEvent event = FamilyBehavioralEvent.builder()
                .family(family)
                .description(request.description())
                .severity(request.severity())
                .occurredAt(request.occurredAt())
                .build();

        FamilyBehavioralEvent saved = repository.save(event);
        return BehavioralEventResponse.from(saved);
    }

    /**
     * Registra la reparación/solución de una fricción existente.
     */
    @Transactional
    public BehavioralEventResponse repair(Long eventId, RepairBehavioralEventRequest request) {
        log.info("[IVR] Registrando reparación para evento conductual ID: {}", eventId);

        FamilyBehavioralEvent event = repository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Evento conductual no encontrado: " + eventId, "NOT_FOUND", HttpStatus.NOT_FOUND));

        if (event.getRepairedAt() != null) {
            throw new BusinessException("Este evento ya se encuentra reparado.", "ALREADY_REPAIRED", HttpStatus.BAD_REQUEST);
        }

        if (request.repairedAt().isBefore(event.getOccurredAt())) {
            throw new BusinessException("La fecha de reparación no puede ser anterior a la ocurrencia de la fricción.", "INVALID_TIMELINE", HttpStatus.BAD_REQUEST);
        }

        event.setRepairedAt(request.repairedAt());
        event.setRepairDescription(request.repairDescription());

        FamilyBehavioralEvent updated = repository.save(event);
        return BehavioralEventResponse.from(updated);
    }

    /**
     * Encuentra todo el historial cronológico de eventos conductuales de una familia.
     */
    @Transactional(readOnly = true)
    public List<BehavioralEventResponse> findByFamily(Long familyId) {
        log.info("[IVR] Recuperando historial de eventos conductuales para familia ID: {}", familyId);
        return repository.findByFamilyIdOrderByOccurredAtDesc(familyId)
                .stream()
                .map(BehavioralEventResponse::from)
                .toList();
    }

    /**
     * Calcula de forma numérica el Índice de Velocidad de Reparación Familiar (IVR).
     * Fórmula:
     *   IVR = (Fricciones Reparadas / Total Fricciones) * Promedio(100 * exp(-0.04 * horas_reparacion))
     */
    @Transactional(readOnly = true)
    public FamilyIvrSummary calculateIvr(Long familyId) {
        log.info("[IVR] Calculando Índice de Velocidad de Reparación Familiar (IVR) para familia ID: {}", familyId);

        List<FamilyBehavioralEvent> events = repository.findByFamilyIdOrderByOccurredAtDesc(familyId);

        if (events.isEmpty()) {
            // Un hogar sin fricciones reportadas mantiene un IVR perfecto (100.0) de forma preventiva.
            return new FamilyIvrSummary(familyId, 0, 0, 0.0, 100.0);
        }

        int totalConflicts = events.size();
        List<FamilyBehavioralEvent> repairedEvents = events.stream()
                .filter(e -> e.getRepairedAt() != null)
                .toList();

        int repairedConflicts = repairedEvents.size();
        if (repairedConflicts == 0) {
            return new FamilyIvrSummary(familyId, totalConflicts, 0, 0.0, 0.0);
        }

        double totalHours = 0.0;
        double sumOfDecayScores = 0.0;

        for (FamilyBehavioralEvent event : repairedEvents) {
            double durationHours = Duration.between(event.getOccurredAt(), event.getRepairedAt()).toMinutes() / 60.0;
            totalHours += durationHours;

            // Decaimiento exponencial: F_decay = exp(-0.04 * horas)
            // Penaliza de forma inteligente el tiempo transcurrido antes de realizar la reparación
            double decayScore = 100.0 * Math.exp(-0.04 * durationHours);
            sumOfDecayScores += decayScore;
        }

        double averageRepairTimeHours = totalHours / repairedConflicts;
        double averageEventScore = sumOfDecayScores / repairedConflicts;

        // El IVR penaliza el porcentaje de conflictos sin reparar y recompensa la velocidad de los reparados
        double repairRatio = (double) repairedConflicts / totalConflicts;
        double rawIvrScore = repairRatio * averageEventScore;

        // Formateo estético a un solo decimal para entrega Premium
        double ivrScoreRounded = Math.round(rawIvrScore * 10.0) / 10.0;
        double avgRepairTimeHoursRounded = Math.round(averageRepairTimeHours * 10.0) / 10.0;

        log.info("[IVR] Cálculo finalizado. Conflictos: {}/{}, Tiempo Promedio: {}h, Score IVR: {}", 
                repairedConflicts, totalConflicts, avgRepairTimeHoursRounded, ivrScoreRounded);

        return new FamilyIvrSummary(familyId, totalConflicts, repairedConflicts, avgRepairTimeHoursRounded, ivrScoreRounded);
    }
}
