package com.integrityfamily.milestone.service;

import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.milestone.domain.Milestone;
import com.integrityfamily.milestone.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final FamilyRepository familyRepository;

    public MilestoneService(MilestoneRepository milestoneRepository, FamilyRepository familyRepository) {
        this.milestoneRepository = milestoneRepository;
        this.familyRepository = familyRepository;
    }

    private static final Map<String, String> MILESTONE_LABELS = Map.of(
        "MES_00_DIAGNOSTICO_BASE", "Inicio - Diagnóstico Base",
        "MES_03_PRIMEROS_CAMBIOS", "3 Meses - Primeros Cambios",
        "MES_06_CONSOLIDACION_INICIAL", "6 Meses - Consolidación Inicial",
        "MES_12_PRIMERA_TRANSFORMACION", "12 Meses - Primera Transformación",
        "MES_18_PROFUNDIZACION", "18 Meses - Profundización",
        "MES_24_MADUREZ_SISTEMA", "24 Meses - Madurez del Sistema",
        "MES_30_CIERRE_SOSTENIMIENTO", "30 Meses - Cierre y Sostenimiento",
        "MES_36_TRANSFORMACION_COMPLETA", "36 Meses - Transformación Completa"
    );

    private static final List<String> MILESTONE_SEQUENCE = List.of(
        "MES_00_DIAGNOSTICO_BASE",
        "MES_03_PRIMEROS_CAMBIOS",
        "MES_06_CONSOLIDACION_INICIAL",
        "MES_12_PRIMERA_TRANSFORMACION",
        "MES_18_PROFUNDIZACION",
        "MES_24_MADUREZ_SISTEMA",
        "MES_30_CIERRE_SOSTENIMIENTO",
        "MES_36_TRANSFORMACION_COMPLETA"
    );

    @Transactional(readOnly = true)
    public List<Milestone> findAll() {
        return milestoneRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public String getCurrentMilestoneLabel(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));
        return MILESTONE_LABELS.getOrDefault(family.getCurrentMilestone(), "Fase Desconocida");
    }

    @Transactional
    public String advanceMilestone(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));
        
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