package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Protocolo de Simulación Sentinel.
 * Valida la transición de estados en el motor de IA y el envío a RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SentinelSimulationService {

    private final FamilyRepository familyRepository;
    private final EvaluationService evaluationService;

    @Transactional
    public String runBurstSimulation(Long familyId) {
        log.info("🚀 [SIMULATION] Iniciando ráfaga técnica para familia: {}", familyId);

        try {
            if (!familyRepository.existsById(familyId)) {
                return "ERROR SDD: Especificación de Familia no encontrada.";
            }

            // 1. ESTADO DE ARMONÍA: Baseline de Alta Integridad (ICF 5.0)
            log.info(">>>> 1/2 Inyectando Baseline: ICF 5.0");
            // SDD: Asegúrate de que evaluationService tenga este método implementado
            evaluationService.processSimulatedResult(familyId, 5.0, false);

            // 2. ESTADO SENTINEL: Trigger de Crisis Crítica (ICF 1.0 + Crisis)
            log.info(">>>> 2/2 Inyectando Trigger: ICF 1.0 + Crisis");
            evaluationService.processSimulatedResult(familyId, 1.0, true);

            return "Simulación completada. Eventos de crisis derivados a RabbitMQ con éxito.";
        } catch (Exception e) {
            log.error("❌ Falla crítica en protocolo de simulación: {}", e.getMessage());
            return "FAILURE: " + e.getMessage();
        }
    }
}


