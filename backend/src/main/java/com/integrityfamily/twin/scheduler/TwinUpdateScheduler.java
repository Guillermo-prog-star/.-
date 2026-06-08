package com.integrityfamily.twin.scheduler;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.twin.service.DigitalTwinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Actualiza el Gemelo Digital de todas las familias cada domingo a las 03:00 AM.
 * Esto asegura que las predicciones de la semana estén frescas cuando la familia
 * abre la app el lunes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TwinUpdateScheduler {

    private final DigitalTwinService twinService;
    private final FamilyRepository   familyRepository;

    @Scheduled(cron = "0 0 3 * * SUN")
    public void updateAllTwins() {
        log.info("[TWIN-SCHEDULER] Actualizando Gemelos Digitales semanales...");
        familyRepository.findAll().forEach(family -> {
            try {
                twinService.compute(family.getId());
            } catch (Exception e) {
                log.warn("[TWIN-SCHEDULER] Error para familia {}: {}", family.getId(), e.getMessage());
            }
        });
        log.info("[TWIN-SCHEDULER] Actualización completada.");
    }
}
