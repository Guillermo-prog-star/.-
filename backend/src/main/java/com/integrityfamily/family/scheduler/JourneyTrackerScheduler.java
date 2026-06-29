package com.integrityfamily.family.scheduler;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.service.JourneyProgressTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Toma un snapshot diario del progreso del viaje para cada familia.
 * Detecta level-ups y envía celebraciones WhatsApp.
 *
 * Horario: 09:00 todos los días (30 min después del radar, para no solapar carga).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JourneyTrackerScheduler {

    private final JourneyProgressTrackerService trackerService;
    private final FamilyRepository             familyRepository;

    @Scheduled(cron = "0 0 9 * * *")
    public void runDailyJourneySnapshot() {
        log.info("[JOURNEY-SCHEDULER] Iniciando snapshot diario del viaje...");

        List<Family> families = familyRepository.findAll();
        AtomicInteger snapped  = new AtomicInteger(0);
        AtomicInteger levelUps = new AtomicInteger(0);
        AtomicInteger errors   = new AtomicInteger(0);

        for (Family family : families) {
            try {
                boolean leveled = trackerService.trackAndCelebrate(family.getId());
                snapped.incrementAndGet();
                if (leveled) levelUps.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
                log.warn("[JOURNEY-SCHEDULER] Error procesando familia {}: {}",
                        family.getId(), e.getMessage());
            }
        }

        // Reintentar celebraciones pendientes de días anteriores
        int retried = 0;
        try {
            retried = trackerService.retryCelebrations();
        } catch (Exception e) {
            log.warn("[JOURNEY-SCHEDULER] Error en reintento de celebraciones: {}", e.getMessage());
        }

        log.info("[JOURNEY-SCHEDULER] Completado — {} snapshots, {} level-ups, {} celebraciones reintentadas, {} errores.",
                snapped.get(), levelUps.get(), retried, errors.get());
    }
}
