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
        Map<String, Object> payload = new HashMap<>();
        payload.put("evaluationId", eval.getId());
        payload.put("familyId", family.getId());
        payload.put("riskLevel", "MEDIUM");
        
        Map<String, Object> eventWrapper = new HashMap<>();
        eventWrapper.put("payload", payload);

        planGenerationService.generatePlanFromEvaluation(eventWrapper);

        // 4. Verificaciones
        List<com.integrityfamily.plan.dto.PlanDtos.PlanResponse> plans = planService.findByFamilyId(family.getId());
        assertFalse(plans.isEmpty(), "Debería haberse creado al menos un plan");
        
        com.integrityfamily.plan.dto.PlanDtos.PlanResponse latestPlan = plans.get(plans.size() - 1);
        assertFalse(latestPlan.tasks().isEmpty(), "El plan debería tener misiones (tasks)");
        
        System.out.println("TEST EXITOSO: Se crearon " + latestPlan.tasks().size() + " misiones automáticas.");
        latestPlan.tasks().forEach(t -> {
            System.out.println("- Misión: " + t.title() + " (Pilar: " + t.pillarName() + ", Hito: " + t.milestoneCode() + ", Miembro: " + t.memberType() + ", Riesgo: " + t.riskType() + ", Generador: " + t.missionGenerator() + ")");
            assertNotNull(t.pillarName(), "pillarName no debe ser nulo");
            assertNotNull(t.milestoneCode(), "milestoneCode no debe ser nulo");
            assertNotNull(t.memberType(), "memberType no debe ser nulo");
            assertNotNull(t.riskType(), "riskType no debe ser nulo");
            assertNotNull(t.missionGenerator(), "missionGenerator no debe ser nulo");
            
            assertFalse(t.pillarName().isBlank(), "pillarName no debe estar vacío");
            assertFalse(t.milestoneCode().isBlank(), "milestoneCode no debe estar vacío");
            assertFalse(t.memberType().isBlank(), "memberType no debe estar vacío");
            assertFalse(t.riskType().isBlank(), "riskType no debe estar vacío");
            assertFalse(t.missionGenerator().isBlank(), "missionGenerator no debe estar vacío");
        });
    }
}
