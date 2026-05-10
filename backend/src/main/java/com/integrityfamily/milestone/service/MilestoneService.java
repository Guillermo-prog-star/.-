package com.integrityfamily.milestone.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Milestone;
import com.integrityfamily.domain.repository.MilestoneRepository;
import com.integrityfamily.domain.repository.ChecklistRepository;
import com.integrityfamily.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;

/**
 * MilestoneService: Motor de Transformación Familiar a 36 meses.
 * Completamente dinámico, conectando directamente con los hitos reales en base de datos.
 * Incluye un protocolo de auto-curación (Self-Healing) para datos legacy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final FamilyRepository familyRepository;
    private final ChecklistRepository checklistRepository;

    @Transactional(readOnly = true)
    public Milestone findById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hito no encontrado con ID: " + id));
    }

    @Transactional
    public Milestone create(Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public Milestone update(Long id, Milestone milestone) {
        Milestone existing = findById(id);
        existing.setTitle(milestone.getTitle());
        existing.setLabel(milestone.getLabel());
        existing.setDurationDays(milestone.getDurationDays());
        existing.setOrderIndex(milestone.getOrderIndex());
        existing.setDescription(milestone.getDescription());
        return milestoneRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!milestoneRepository.existsById(id)) {
            throw new NotFoundException("No se puede eliminar: Hito no existe.");
        }
        milestoneRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Milestone> findAll() {
        return milestoneRepository.findAll().stream()
                .sorted(Comparator.comparingInt(m -> m.getOrderIndex() != null ? m.getOrderIndex() : 99))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canAdvance(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        
        String currentMilestone = family.getCurrentMilestone();
        
        // [SDD Spec] Gatekeeping: Contamos tareas pendientes asociadas al hito actual.
        // Si hay tareas críticas (source = currentMilestone) pendientes, no se puede avanzar.
        long pending = checklistRepository.countByFamilyIdAndSourceAndCompletedFalse(familyId, currentMilestone);
        
        log.info("🔍 [MILESTONE-SERVICE] Evaluando avance para familia {}. Hito actual: {}. Tareas pendientes: {}", 
                familyId, currentMilestone, pending);
        
        return pending == 0;
    }

    @Transactional(readOnly = true)
    public String getCurrentMilestoneLabel(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        
        String code = family.getCurrentMilestone();
        return milestoneRepository.findByCode(code)
                .map(m -> m.getLabel() != null ? m.getLabel() : m.getTitle())
                .orElse("Hito " + code);
    }

    @Transactional
    public String advanceMilestone(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));

        if (!canAdvance(familyId)) {
            throw new IllegalStateException("No se puede avanzar: Existen tareas pendientes para el hito actual.");
        }

        String current = family.getCurrentMilestone();
        
        // Obtener la secuencia de hitos real y ordenada desde la base de datos
        List<Milestone> sequence = milestoneRepository.findAll().stream()
                .sorted(Comparator.comparingInt(m -> m.getOrderIndex() != null ? m.getOrderIndex() : 99))
                .toList();

        int currentIndex = -1;
        for (int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).getCode().equalsIgnoreCase(current)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            // Self-Healing: Si el hito actual es legacy o desconocido, lo inicializamos en el primer hito de la secuencia
            if (!sequence.isEmpty()) {
                String firstCode = sequence.get(0).getCode();
                family.setCurrentMilestone(firstCode);
                familyRepository.save(family);
                log.info("🏥 [SELF-HEALING] Hito desconocido '{}' actualizado automáticamente al primer hito moderno '{}' para la familia ID: {}.", 
                        current, firstCode, familyId);
                return firstCode;
            }
        } else if (currentIndex < sequence.size() - 1) {
            String next = sequence.get(currentIndex + 1).getCode();
            family.setCurrentMilestone(next);
            familyRepository.save(family);
            log.info("🚀 [MILESTONE-SERVICE] Familia {} avanzada del hito '{}' al '{}' de forma exitosa.", 
                    familyId, current, next);
            return next;
        }
        
        log.warn("⚠️ [MILESTONE-SERVICE] Familia {} ya se encuentra en el hito terminal '{}'.", familyId, current);
        return current;
    }
}
