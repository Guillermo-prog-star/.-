package com.integrityfamily.report.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.common.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SDD-REP-AUTO-01: Automated Semi-Annual Reporting Service.
 * Periodically identifies families due for an executive report and triggers the notification flow.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutomatedReportingService {

    private final FamilyRepository familyRepository;
    private final WhatsAppService whatsappService;

    /**
     * Runs daily at midnight to check for reporting milestones.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processScheduledReports() {
        log.info("🤖 [AUTO-REPORT] Iniciando escaneo diario de hitos de reportabilidad...");
        
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        
        // Buscamos familias que:
        // 1. Nunca han recibido un reporte y se crearon hace más de 6 meses.
        // 2. Recibieron su último reporte hace más de 6 meses.
        List<Family> familiesDue = familyRepository.findAll().stream()
                .filter(f -> isDueForReport(f, sixMonthsAgo))
                .toList();

        log.info("🤖 [AUTO-REPORT] Se identificaron {} familias candidatas para reporte semestral.", familiesDue.size());

        for (Family family : familiesDue) {
            sendAutomatedReport(family);
        }
    }

    private boolean isDueForReport(Family family, LocalDateTime threshold) {
        if (family.getLastReportSentAt() == null) {
            return family.getCreatedAt().isBefore(threshold);
        }
        return family.getLastReportSentAt().isBefore(threshold);
    }

    private void sendAutomatedReport(Family family) {
        try {
            log.info("🤖 [AUTO-REPORT] Generando aviso de reporte para la familia {}", family.getId());
            
            String message = String.format(
                "✨ *REPORTE SEMESTRAL DE INTEGRIDAD*\n\n" +
                "Hola familia %s, han completado un ciclo de 6 meses de transformación. " +
                "Su nuevo Reporte Ejecutivo ya está disponible en su Dashboard. " +
                "¡Sigan cultivando su integridad!", 
                family.getName()
            );

            whatsappService.sendToFamily(family, message);
            
            family.setLastReportSentAt(LocalDateTime.now());
            familyRepository.save(family);
            
            log.info("🤖 [AUTO-REPORT] Reporte notificado exitosamente a la familia {}", family.getId());
        } catch (Exception e) {
            log.error("❌ [AUTO-REPORT] Error al procesar reporte para familia {}", family.getId(), e);
        }
    }
}


