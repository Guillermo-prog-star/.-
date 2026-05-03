package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protocolo de SimulaciÃƒÂ³n Sentinel.
 * Valida la transiciÃƒÂ³n de estados en el motor de IA y el envÃƒÂ­o a RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SentinelSimulationService {

    private final FamilyRepository familyRepository;
    private final EvaluationService evaluationService;

    @Transactional
    public String runBurstSimulation(Long familyId) {
        log.info("Ã°Å¸Å¡â‚¬ [SIMULATION] Iniciando rÃƒÂ¡faga tÃƒÂ©cnica para familia: {}", familyId);

        try {
            if (!familyRepository.existsById(familyId)) {
                return "ERROR SDD: EspecificaciÃƒÂ³n de Familia no encontrada.";
            }

            // 1. ESTADO DE ARMONÃƒÂA: Baseline de Alta Integridad (ICF 5.0)
            log.info(">>>> 1/2 Inyectando Baseline: ICF 5.0");
            // SDD: AsegÃƒÂºrate de que evaluationService tenga este mÃƒÂ©todo implementado
            evaluationService.processSimulatedResult(familyId, 5.0, false);

            // 2. ESTADO SENTINEL: Trigger de Crisis CrÃƒÂ­tica (ICF 1.0 + Crisis)
            log.info(">>>> 2/2 Inyectando Trigger: ICF 1.0 + Crisis");
            evaluationService.processSimulatedResult(familyId, 1.0, true);

            return "SimulaciÃƒÂ³n completada. Eventos de crisis derivados a RabbitMQ con ÃƒÂ©xito.";
        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ Falla crÃƒÂ­tica en protocolo de simulaciÃƒÂ³n: {}", e.getMessage());
            return "FAILURE: " + e.getMessage();
        }
    }
}


