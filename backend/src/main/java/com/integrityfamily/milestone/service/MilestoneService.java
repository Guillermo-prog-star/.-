package com.integrityfamily.milestone.service;

import com.integrityfamily.domain.Family; // REQUERIDO: Sanar MilestoneService:52,59
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Milestone;
import com.integrityfamily.domain.repository.MilestoneRepository;
import com.integrityfamily.domain.repository.ChecklistRepository;
import com.integrityfamily.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * MilestoneService: Motor de TransformaciÃƒÂ³n Familiar a 36 meses.
 * Sincronizado para sanar MilestoneController y visibilidad de dominio.
 */
@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final FamilyRepository familyRepository;

    private static final Map<String, String> MILESTONE_LABELS = Map.of(
            "MES_00_DIAGNOSTICO", "Inicio - Diagnóstico Base",
            "MES_03_PRIMEROS_CAMBIOS", "3 Meses - Primeros Cambios",
            "MES_06_CONSOLIDACION_INICIAL", "6 Meses - Consolidación Inicial",
            "MES_12_PRIMERA_TRANSFORMACION", "12 Meses - Primera Transformación",
            "MES_18_PROFUNDIZACION", "18 Meses - Profundización",
            "MES_24_MADUREZ_SISTEMA", "24 Meses - Madurez del Sistema",
            "MES_30_CIERRE_SOSTENIMIENTO", "30 Meses - Cierre y Sostenimiento",
            "MES_36_TRANSFORMACION_COMPLETA", "36 Meses - Transformación Completa");

    private static final List<String> MILESTONE_SEQUENCE = List.of(
            "MES_00_DIAGNOSTICO",
            "MES_03_PRIMEROS_CAMBIOS",
            "MES_06_CONSOLIDACION_INICIAL",
            "MES_12_PRIMERA_TRANSFORMACION",
            "MES_18_PROFUNDIZACION",
            "MES_24_MADUREZ_SISTEMA",
            "MES_30_CIERRE_SOSTENIMIENTO",
            "MES_36_TRANSFORMACION_COMPLETA");

    // --- MÃƒâ€°TODOS REQUERIDOS POR MILESTONECONTROLLER ---

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
        // AquÃƒÂ­ se pueden sincronizar mÃƒÂ¡s campos (sortOrder, description, etc.)
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
        return milestoneRepository.findAll();
    }

    private final ChecklistRepository checklistRepository;

    // --- LÃƒâ€œGICA DE SECUENCIA TERRITORIAL ---

    @Transactional(readOnly = true)
    public boolean canAdvance(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        
        String currentMilestone = family.getCurrentMilestone();
        
        // [SDD Spec] Gatekeeping: Contamos tareas pendientes asociadas al hito actual.
        // Si hay tareas crÃƒÂ­ticas (source = currentMilestone) pendientes, no se puede avanzar.
        long pending = checklistRepository.countByFamilyIdAndSourceAndCompletedFalse(familyId, currentMilestone);
        
        return pending == 0;
    }

    @Transactional(readOnly = true)
    public String getCurrentMilestoneLabel(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        return MILESTONE_LABELS.getOrDefault(family.getCurrentMilestone(), "Fase Desconocida");
    }

    @Transactional
    public String advanceMilestone(Long familyId) {
        if (!canAdvance(familyId)) {
            throw new IllegalStateException("No se puede avanzar: Existen tareas pendientes para el hito actual.");
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));

        String current = family.getCurrentMilestone();
        int currentIndex = MILESTONE_SEQUENCE.indexOf(current);

        if (currentIndex != -1 && currentIndex < MILESTONE_SEQUENCE.size() - 1) {
            String next = MILESTONE_SEQUENCE.get(currentIndex + 1);
            family.setCurrentMilestone(next);
            familyRepository.save(family);
            return next;
        }
        return current;
    }
}


