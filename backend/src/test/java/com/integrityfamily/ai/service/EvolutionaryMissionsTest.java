package com.integrityfamily.ai.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.plan.service.PlanGenerationService;
import com.integrityfamily.plan.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EvolutionaryMissionsTest {

    @Autowired
    private PlanGenerationService planGenerationService;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private PlanService planService;

    @Test
    @Transactional
    void testAutomaticMissionGeneration() {
        // 1. Setup Family y Miembro de forma independiente
        Family family = Family.builder()
                .name("Familia Test Evolutiva")
                .familyCode("TEST-EVO-001")
                .currentMilestone("MES_00_DIAGNOSTICO")
                .build();
        family = familyRepository.save(family);
        
        // 2. Setup Evaluation with dimensions
        Evaluation eval = new Evaluation();
        eval.setFamily(family);
        eval.setIcf(45.0);
        eval.setStatus(EvaluationStatus.FINALIZED);
        
        EvaluationDimensionScore ds = new EvaluationDimensionScore();
        ds.setEvaluation(eval);
        ds.setDimensionName("EMOCIONES");
        ds.setScore(30.0);
        eval.getDimensionScores().add(ds);
        
        evaluationRepository.save(eval);

        // 3. Trigger Plan Generation (Simulando evento de RabbitMQ)
        Map<String, Object> event = new HashMap<>();
        event.put("evaluationId", eval.getId());
        event.put("familyId", family.getId());
        event.put("riskLevel", "MEDIUM");

        planGenerationService.generatePlanFromEvaluation(event);

        // 4. Verificaciones
        List<Plan> plans = planService.findByFamilyId(family.getId());
        assertFalse(plans.isEmpty(), "Debería haberse creado al menos un plan");
        
        Plan latestPlan = plans.get(plans.size() - 1);
        assertFalse(latestPlan.getTasks().isEmpty(), "El plan debería tener misiones (tasks)");
        
        System.out.println("TEST EXITOSO: Se crearon " + latestPlan.getTasks().size() + " misiones automáticas.");
        latestPlan.getTasks().forEach(t -> {
            System.out.println("- Misión: " + t.getTitle() + " (Vence: " + t.getDueDate() + ")");
        });
    }
}
