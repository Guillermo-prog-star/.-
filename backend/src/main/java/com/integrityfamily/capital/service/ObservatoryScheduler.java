package com.integrityfamily.capital.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * Job mensual del Observatorio del Desarrollo Familiar.
 *
 * Se ejecuta el día 1 de cada mes a la 01:00 para procesar el mes anterior.
 * Si el snapshot ya existe para ese mes, ObservatoryService lo actualizará
 * (operación idempotente — seguro relanzar manualmente en cualquier momento).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservatoryScheduler {

    private final ObservatoryService observatoryService;

    /** Día 1 de cada mes a las 01:00 — agrega el mes anterior */
    @Scheduled(cron = "0 0 1 1 * *")
    public void generatePreviousMonth() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("[ObservatoryScheduler] Iniciando agregación mensual para {}", previousMonth);
        try {
            var snapshot = observatoryService.generateForMonth(previousMonth);
            log.info("[ObservatoryScheduler] Snapshot generado correctamente: id={}, mes={}, familias={}",
                    snapshot.getId(), previousMonth, snapshot.getFamiliesCount());
        } catch (Exception e) {
            log.error("[ObservatoryScheduler] Error generando snapshot para {}: {}", previousMonth, e.getMessage(), e);
        }
    }
}
