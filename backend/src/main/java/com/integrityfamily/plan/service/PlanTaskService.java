package com.integrityfamily.plan.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD SPEC: Motor de Ejecución de Planes Harmonizado.
 * Refactorizado para usar ImprovementPlan y la estructura de dominio centralizada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanTaskService {

    private final ImprovementPlanRepository planRepository;
    private final FamilyRepository familyRepository;

    @Transactional
    public void createTasksFromAi(Long familyId, List<String> tasks) {
        log.info("📌 [PLAN-TASK] Iniciando persistencia de {} tareas para familia ID: {}", tasks.size(), familyId);

        ImprovementPlan plan = planRepository.findByFamilyId(familyId).stream()
                .findFirst()
                .orElseGet(() -> createDefaultPlan(familyId));

        tasks.forEach(taskDescription -> {
            PlanTask task = PlanTask.builder()
                    .title("Misión Sugerida por IA")
                    .description(taskDescription)
                    .completed(false)
                    .plan(plan)
                    .build();
            plan.getTasks().add(task);
        });

        planRepository.save(plan);
        log.info("✅ [PLAN-TASK] Sincronización exitosa.");
    }

    private ImprovementPlan createDefaultPlan(Long familyId) {
        log.info("🆕 [PLAN-TASK] Generando ImprovementPlan base.");
        return ImprovementPlan.builder()
                .family(familyRepository.getReferenceById(familyId))
                .title("Estrategia de Integridad Familiar")
                .description("Generado automáticamente por el motor Sentinel.")
                .build();
    }
}
