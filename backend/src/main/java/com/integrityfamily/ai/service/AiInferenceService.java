package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.repository.CriticalDayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiInferenceService {

    private final AiProvider aiProvider;
    private final FamilyRepository familyRepository;
    private final CriticalDayRepository criticalDayRepository;
    private final ContextSynthesizer contextSynthesizer;

    @RabbitListener(queues = "q.ai.inference")
    public void handleCrisisSignal(String familyId) {
        log.info("Ã°Å¸â€Â¥ [AI-INFERENCE] SEÃƒâ€˜AL CAPTURADA: ID {}", familyId);

        try {
            Long id = Long.parseLong(familyId.trim());
            Family family = familyRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Familia no encontrada: " + id, "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

            log.info("Ã°Å¸Â§Â  Sintetizando contexto para: {}", family.getName());

            // 1. Obtener contexto completo (ICF, miembros, historia)
            AiContext context = contextSynthesizer.synthesize(family, "CRISIS");

            // 2. Solicitar inferencia real a Claude
            String response = aiProvider.generateResponse(
                "ALERTA SENTINEL: Se ha detectado una crisis en el nodo. Genera una guÃƒÂ­a de contenciÃƒÂ³n inmediata.", 
                context
            );

            log.info("Ã¢Å“â€¦ INFERENCIA RECIBIDA. Persistiendo en CriticalDay...");

            // 3. Persistir usando el Repositorio (Sincronizado con el cambio en schema.sql)
            CriticalDay criticalDay = CriticalDay.builder()
                    .familyId(id)
                    .category("SENTINEL_ALERT")
                    .emotion("SDD_CRISIS_DETECTADA")
                    .aiContainmentGuide(response)
                    .createdAt(LocalDateTime.now())
                    .build();

            criticalDayRepository.save(criticalDay);

            log.info("Ã°Å¸â€™Â¾ [AI-INFERENCE] Flujo completado con ÃƒÂ©xito para familia {}", family.getName());

        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ FALLO CRÃƒÂTICO EN PROCESO DE IA: {}", e.getMessage(), e);
        }
    }
}


