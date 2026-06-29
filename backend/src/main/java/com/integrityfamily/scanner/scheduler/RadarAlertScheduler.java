package com.integrityfamily.scanner.scheduler;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.scanner.service.RadarAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ejecuta el Radar de Señales Sutiles sobre todas las familias activas una vez al día
 * y envía alertas WhatsApp a las que presenten señales de alta intensidad.
 *
 * Horario: 08:30 todos los días (zona horaria del servidor).
 * Las familias sin WhatsApp configurado se analizan igualmente y se loguean.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RadarAlertScheduler {

    private final RadarAlertService alertService;
    private final FamilyRepository familyRepository;

    @Scheduled(cron = "0 30 8 * * *")
    public void runDailyRadarScan() {
        log.info("[RADAR-SCHEDULER] Iniciando escaneo diario de señales sutiles...");

        List<Family> families = familyRepository.findAll();
        AtomicInteger scanned = new AtomicInteger(0);
        AtomicInteger alerted = new AtomicInteger(0);
        AtomicInteger errors  = new AtomicInteger(0);

        for (Family family : families) {
            try {
                boolean sent = alertService.checkAndAlert(family.getId());
                scanned.incrementAndGet();
                if (sent) alerted.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
                log.warn("[RADAR-SCHEDULER] Error escaneando familia {}: {}",
                        family.getId(), e.getMessage());
            }
        }

        log.info("[RADAR-SCHEDULER] Escaneo completado — {} familias analizadas, {} alertas enviadas, {} errores.",
                scanned.get(), alerted.get(), errors.get());
    }
}
