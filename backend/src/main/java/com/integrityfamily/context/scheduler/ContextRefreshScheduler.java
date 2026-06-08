package com.integrityfamily.context.scheduler;

import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContextRefreshScheduler {

    private final FamilyContextEngine contextEngine;
    private final FamilyRepository    familyRepository;

    /** Refresca el contexto de todas las familias cada 4 horas. */
    @Scheduled(cron = "0 0 */4 * * *")
    public void refreshAll() {
        log.info("[CTX-SCHEDULER] Refrescando contexto de todas las familias...");
        familyRepository.findAll().forEach(family -> {
            try {
                contextEngine.compute(family.getId(), true);
            } catch (Exception e) {
                log.warn("[CTX-SCHEDULER] Error procesando familia {}: {}", family.getId(), e.getMessage());
            }
        });
        log.info("[CTX-SCHEDULER] Contexto actualizado.");
    }
}
