package com.integrityfamily.plan.scheduler;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.PlanTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PlanComplianceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlanComplianceScheduler.class);

    private final PlanTaskRepository taskRepository;
    private final FamilyRepository familyRepository;
    private final WhatsAppService whatsappService;

    public PlanComplianceScheduler(PlanTaskRepository taskRepository, 
                                   FamilyRepository familyRepository, 
                                   WhatsAppService whatsappService) {
        this.taskRepository = taskRepository;
        this.familyRepository = familyRepository;
        this.whatsappService = whatsappService;
    }

    /**
     * MONITOR DE HITOS TRIMESTRALES
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkFamilyMilestones() {
        log.info("Ã°Å¸â€¢â€™ [MILESTONE-CLOCK] Iniciando auditorÃƒÂ­a trimestral de familias...");
        LocalDateTime now = LocalDateTime.now();

        List<Family> familiesAtRiskOrDue = familyRepository.findByNextEvaluationAtBeforeOrNextEvaluationAtIsNull(now);

        for (Family family : familiesAtRiskOrDue) {
            if (family.getNextEvaluationAt() == null) {
                family.setNextEvaluationAt(family.getCreatedAt().plusMonths(3));
                familyRepository.save(family);
                continue;
            }

            long months = ChronoUnit.MONTHS.between(family.getCreatedAt(), now);
            int milestoneNumber = (int) (months / 3) + 1;
            String milestoneLabel = "HITO_" + milestoneNumber;

            log.info("Ã°Å¸â€œÂ [MILESTONE-HIT] Familia {} alcanzÃƒÂ³ el hito: {}", family.getName(), milestoneLabel);

            family.setCurrentMilestone(milestoneLabel);
            family.setNextEvaluationAt(now.plusMonths(3));
            familyRepository.save(family);

            if (family.getWhatsapp() != null && !family.getWhatsapp().isBlank()) {
                String message = "Ã°Å¸Å’Å¸ INTEGRITY FAMILY: Ã‚Â¡Felicidades Familia " + family.getName() + "! Su hito '" + milestoneLabel + "' ha llegado. " +
                        "Es el momento de realizar su diagnÃƒÂ³stico trimestral.";
                whatsappService.sendMessage(family.getWhatsapp(), message);
            }
        }
    }

    /**
     * MONITOR DE CUMPLIMIENTO DIARIO
     */
    @Scheduled(cron = "0 0 0/4 * * *")
    public void checkTaskCompliance() {
        log.info("Ã°Å¸â€Â [COMPLIANCE-CLOCK] Revisando tareas vencidas...");
        
        List<PlanTask> allTasks = taskRepository.findAll();
        for (PlanTask task : allTasks) {
            if (Boolean.FALSE.equals(task.getCompleted()) && 
                task.getDueDate() != null && 
                task.getDueDate().isBefore(LocalDateTime.now())) {
                
                String whatsapp = task.getPlan().getFamily().getWhatsapp();
                if (whatsapp != null && !whatsapp.isBlank()) {
                    String message = "Ã¢Å¡Â Ã¯Â¸Â NotificaciÃƒÂ³n: Tarea '" + task.getTitle() + "' pendiente.";
                    whatsappService.sendMessage(whatsapp, message);
                }
            }
        }
    }
}


