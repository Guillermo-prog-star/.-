package com.integrityfamily.cognitive.scheduler;

import com.integrityfamily.cognitive.service.FamilyReflectionService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SDD Fase 3 — Scheduler semanal del sistema cognitivo familiar.
 *
 * Ejecuta la reflexión autónoma para todas las familias activas cada domingo a las 03:00.
 * El objetivo es que el sistema "aprenda de la semana" independientemente de si hubo
 * una evaluación reciente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CognitiveReflectionScheduler {

    private final FamilyReflectionService familyReflectionService;
    private final FamilyRepository familyRepository;

    @Scheduled(cron = "0 0 3 * * SUN")
    public void runWeeklyReflection() {
        List<Family> families = familyRepository.findAll();
        log.info("🌀 [SCHEDULER] Iniciando reflexión autónoma semanal para {} familias.", families.size());

        int success = 0;
        int errors = 0;

        for (Family family : families) {
            try {
                FamilyReflectionService.ReflectionReport report =
                        familyReflectionService.reflect(family.getId());
                log.debug("✅ [SCHEDULER] Familia {} — Efectividad: {} | Abandono: {}",
                        family.getId(),
                        report.effectiveness().level(),
                        report.abandonmentRisk().level());
                success++;
            } catch (Exception e) {
                log.warn("⚠️ [SCHEDULER] Error en reflexión para familia {}: {}", family.getId(), e.getMessage());
                errors++;
            }
        }

        log.info("🏁 [SCHEDULER] Reflexión semanal completada. Exitosas: {} | Errores: {}", success, errors);
    }
}
