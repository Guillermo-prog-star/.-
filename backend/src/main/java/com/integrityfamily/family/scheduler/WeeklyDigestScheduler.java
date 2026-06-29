package com.integrityfamily.family.scheduler;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.service.FamilyWeeklyDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Envía el resumen semanal de progreso a todas las familias con WhatsApp configurado.
 *
 * Horario: sábados a las 09:30 — cuando la familia suele tener tiempo de leer.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WeeklyDigestScheduler {

    private final FamilyWeeklyDigestService digestService;
    private final FamilyRepository          familyRepository;

    @Scheduled(cron = "0 30 9 * * SAT")
    public void runWeeklyDigest() {
        log.info("[DIGEST-SCHEDULER] Iniciando envío de resúmenes semanales...");

        List<Family> families = familyRepository.findAll();
        AtomicInteger sent   = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        for (Family family : families) {
            try {
                boolean ok = digestService.sendDigest(family.getId());
                if (ok) sent.incrementAndGet();
                else    skipped.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
                log.warn("[DIGEST-SCHEDULER] Error procesando familia {}: {}",
                        family.getId(), e.getMessage());
            }
        }

        log.info("[DIGEST-SCHEDULER] Completado — {} enviados, {} omitidos (sin WhatsApp), {} errores.",
                sent.get(), skipped.get(), errors.get());
    }
}
