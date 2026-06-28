package com.integrityfamily.admin.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityWatchdogService {

    private final FamilyRepository familyRepository;
    private final FeedbackRepository feedbackRepository;
    private final AdminAlertRepository alertRepository;

    /**
     * Ciclo de Vigilancia de Datos: Ejecuci?n cada 5 minutos.
     * Escanea anomal?as cr?ticas en los 50 nodos Alfa.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scanForAnomalies() {
        log.debug("🛡️ [WATCHDOG] Escaneando anomalías en la red familiar...");

        // 1. Detectar Crisis Sentinel Activas (Optimizado)
        List<Family> activeCrises = familyRepository.findBySentinelActiveTrue();

        for (Family f : activeCrises) {
            String alertTitle = "CRISIS ACTIVA: " + f.getFamilyCode();
            String alertMessage = "Protocolo Sentinel detectado en el hito " + f.getCurrentMilestone();
            
            if (alertRepository.findByTitleAndMessage(alertTitle, alertMessage).isEmpty()) {
                alertRepository.save(AdminAlert.builder()
                        .title(alertTitle)
                        .message(alertMessage)
                        .severity("CRITICAL")
                        .build());
                log.warn("🚨 [WATCHDOG-ALERT] Alerta crítica registrada para William: {}", alertTitle);
            }
        }

        // 2. Detectar Feedback Altamente Negativo (Optimizado)
        java.time.LocalDateTime fiveMinutesAgo = java.time.LocalDateTime.now().minusMinutes(5);
        List<Feedback> criticalFeedback = 
                feedbackRepository.findByScoreLessThanEqualAndCreatedAtAfter(1, fiveMinutesAgo);

        for (Feedback fb : criticalFeedback) {
            String fbTitle = "FEEDBACK CR�? TICO: " + fb.getFamily().getFamilyCode();
            if (alertRepository.findByTitleAndMessage(fbTitle, fb.getComment()).isEmpty()) {
                alertRepository.save(AdminAlert.builder()
                        .title(fbTitle)
                        .message("Puntuación: 1 estrella. Comentario: " + fb.getComment())
                        .severity("WARNING")
                        .build());
            }
        }
    }
}


