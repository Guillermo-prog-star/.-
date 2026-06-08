package com.integrityfamily.ritual.scheduler;

import com.integrityfamily.ritual.service.RitualEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RitualScheduler {

    private final RitualEngineService ritualEngineService;

    /**
     * Detecta rituales a las 07:00 AM todos los días.
     * Corre antes de que la familia despierte para que al abrir la app
     * ya encuentren el ritual del día.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void detectDailyRituals() {
        log.info("[RITUAL-SCHEDULER] Iniciando detección diaria de rituales...");
        try {
            ritualEngineService.detectAndCreateRituals();
            log.info("[RITUAL-SCHEDULER] Detección completada.");
        } catch (Exception e) {
            log.error("[RITUAL-SCHEDULER] Error en detección: {}", e.getMessage());
        }
    }
}
