package com.integrityfamily.scanner.scheduler;

import com.integrityfamily.scanner.domain.InferenceRecord;
import com.integrityfamily.scanner.repository.InferenceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * IF-STAB: Scheduler de estabilización epistemológica.
 *
 * Convierte registros INFERRED → STABILIZED cuando han pasado
 * {@value #STABILIZATION_DAYS} días sin que llegue evidencia contradictoria.
 *
 * Definición de "estabilizado": un registro INFERRED que no ha sido
 * marcado REVISED en el período de ventana → se considera confirmado
 * por ausencia de contradicción. Esto es evidencia negativa formal: si
 * hubiera habido una nueva evaluación con datos diferentes, se habría
 * generado un nuevo INFERRED con evidenceHash distinto.
 *
 * Corre diariamente a las 02:00 AM para no competir con otros schedulers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StabilizationScheduler {

    static final int STABILIZATION_DAYS = 7;

    private final InferenceRecordRepository inferenceRecordRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void stabilizeInferredRecords() {
        Instant cutoff = Instant.now().minus(STABILIZATION_DAYS, ChronoUnit.DAYS);
        List<InferenceRecord> candidates = inferenceRecordRepository.findInferredBefore(cutoff);

        if (candidates.isEmpty()) {
            log.debug("[IF-STAB] Sin registros INFERRED elegibles para estabilización.");
            return;
        }

        int stabilized = 0;
        for (InferenceRecord record : candidates) {
            record.setEpistemicState("STABILIZED");
            record.setStabilizedAt(Instant.now());
            stabilized++;
        }

        inferenceRecordRepository.saveAll(candidates);
        log.info("[IF-STAB] {} registros estabilizados (INFERRED → STABILIZED, ventana {} días).",
                stabilized, STABILIZATION_DAYS);
    }
}
