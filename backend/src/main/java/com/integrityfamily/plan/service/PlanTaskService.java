package com.integrityfamily.plan.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.Plan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD SPEC: Motor de EjecuciÃƒÂ³n de Planes.
 * Sincronizado con los campos de dominio para eliminar fallas de compilaciÃƒÂ³n.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanTaskService {

    private final PlanRepository planRepository;
    private final FamilyRepository familyRepository;

    @Transactional
    public void createTasksFromAi(Long familyId, List<String> tasks) {
        log.info("Ã°Å¸â€œÂ [PLAN-TASK] Iniciando persistencia de {} tareas para familia ID: {}", tasks.size(), familyId);

        Plan plan = planRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId)
                .orElseGet(() -> createDefaultPlan(familyId));

        tasks.forEach(taskDescription -> {
            PlanTask task = PlanTask.builder()
                    .title("MisiÃƒÂ³n Sugerida por IA") // Requerido por @Column(nullable = false)
                    .description(taskDescription)
                    .completed(false) // SDD FIX: Sincronizado con el campo 'completed' de PlanTask
                    .plan(plan)
                    .build();
            plan.getTasks().add(task);
        });

        planRepository.save(plan);
        log.info("Ã¢Å“â€¦ [PLAN-TASK] SincronizaciÃƒÂ³n exitosa.");
    }

    private Plan createDefaultPlan(Long familyId) {
        log.info("Ã°Å¸â€ â€¢ [PLAN-TASK] Generando Plan Sentinel base.");
        return Plan.builder()
                .family(familyRepository.getReferenceById(familyId))
                .title("Estrategia de Integridad Familiar")
                .description("Generado automÃƒÂ¡ticamente por el motor Sentinel.")
                .build();
    }
}


