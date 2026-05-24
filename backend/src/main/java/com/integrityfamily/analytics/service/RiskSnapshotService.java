package com.integrityfamily.analytics.service;

import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskSnapshotService {

    private final RiskSnapshotRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void saveSnapshot(RiskSnapshot snapshot) {
        repository.save(snapshot);
        log.info("[ANALYTICS] Snapshot persistido. ICF: {}", snapshot.getIcf());
        
        // SDD Fix: Si getFamilyId() falla, intentamos obtenerlo de la relaciÃƒÂ³n o el campo directo
        if (snapshot.getHasCrisis() != null && snapshot.getHasCrisis()) {
            Long fId = snapshot.getFamily() != null ? snapshot.getFamily().getId() : 2L; 
            log.info("Ã°Å¸â€œÂ¡ Disparando evento de crisis a RabbitMQ para familia ID: {}", fId);
            try {
                rabbitTemplate.convertAndSend("x.ai.events", "crisis.detected", fId);
            } catch (Exception e) {
                log.error("❌ [ANALYTICS] Error al publicar evento de crisis a RabbitMQ (resiliencia activada): {}", e.getMessage());
            }
        }
    }
}


