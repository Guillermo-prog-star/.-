package com.integrityfamily.plan.scheduler;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.ImprovementPlan;
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
        log.info("⏰ [MILESTONE-CLOCK] Iniciando auditoría trimestral de familias...");
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

            log.info("📍 [MILESTONE-HIT] Familia {} alcanzó el hito: {}", family.getName(), milestoneLabel);

            family.setCurrentMilestone(milestoneLabel);
            family.setNextEvaluationAt(now.plusMonths(3));
            familyRepository.save(family);

            if (family.getWhatsapp() != null && !family.getWhatsapp().isBlank()) {
                String message = "🌟 INTEGRITY FAMILY: ¡Felicidades Familia " + family.getName() + "! Su hito '" + milestoneLabel + "' ha llegado. " +
                        "Es el momento de realizar su diagnóstico trimestral.";
                whatsappService.sendMessage(family.getWhatsapp(), message);
            }
        }
    }

    /**
     * MONITOR DE CUMPLIMIENTO DIARIO
     */
    @Scheduled(cron = "0 0 0/4 * * *")
    public void checkTaskCompliance() {
        log.info("🔍 [COMPLIANCE-CLOCK] Revisando tareas vencidas...");
        
        List<PlanTask> allTasks = taskRepository.findAll();
        for (PlanTask task : allTasks) {
            // Sincronización con el nuevo ImprovementPlan y PlanTask
            if (!task.isCompleted() && 
                task.getDueDate() != null && 
                task.getDueDate().isBefore(LocalDateTime.now())) {
                
                ImprovementPlan plan = task.getPlan();
                if (plan != null && plan.getFamily() != null) {
                    String whatsapp = plan.getFamily().getWhatsapp();
                    if (whatsapp != null && !whatsapp.isBlank()) {
                        String message = "⚠️ Notificación: Tarea '" + task.getTitle() + "' pendiente.";
                        whatsappService.sendMessage(whatsapp, message);
                    }
                }
            }
        }
    }

    /**
     * ENVIADOR SEMANAL DE RECORDATORIOS DE HÁBITOS (Mentoría Interactiva WhatsApp)
     * Se ejecuta todos los lunes a las 9:00 AM para motivar y facilitar el envío de evidencias.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void sendWeeklyHabitReminders() {
        log.info("⏰ [HABIT-REMINDER-CLOCK] Iniciando despacho semanal de mentoría de hábitos...");
        
        List<PlanTask> pendingTasks = taskRepository.findAll().stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> t.getDueDate() != null && t.getDueDate().isAfter(LocalDateTime.now()) && t.getDueDate().isBefore(LocalDateTime.now().plusDays(7)))
                .toList();

        if (pendingTasks.isEmpty()) {
            log.info("✨ [HABIT-REMINDER-CLOCK] No hay microacciones programadas para vencer esta semana.");
            return;
        }

        // Agrupar tareas por familia para enviar un único mensaje consolidado
        java.util.Map<Family, List<PlanTask>> familyTasks = pendingTasks.stream()
                .filter(t -> t.getPlan() != null && t.getPlan().getFamily() != null)
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getPlan().getFamily()));

        familyTasks.forEach((family, tasks) -> {
            if (family.getWhatsapp() == null || family.getWhatsapp().isBlank()) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🏡 *ACOMPAÑAMIENTO INTEGRITY FAMILY* 🏡\n\n");
            sb.append(String.format("¡Hola Familia *%s*! Esperamos que estén teniendo una semana llena de unión y alegría. 🌱\n\n", family.getName()));
            sb.append("Para seguir fortaleciendo la armonía en su hogar, les animamos a enfocar su energía en las siguientes microacciones de su plan activo:\n\n");

            for (PlanTask task : tasks) {
                String emoji = switch (task.getDimension() != null ? task.getDimension().toUpperCase() : "GENERAL") {
                    case "EMOCIONES" -> "🧠";
                    case "COMUNICACION" -> "💬";
                    case "HABITOS" -> "📅";
                    case "TIEMPOS" -> "⏳";
                    default -> "✨";
                };
                sb.append(String.format("%s *%s*\n", emoji, task.getTitle()));
                if (task.getAccionConcreta() != null && !task.getAccionConcreta().isBlank()) {
                    sb.append(String.format("   _Misión:_ %s\n", task.getAccionConcreta()));
                }
                if (task.getEvidenciaRequerida() != null && !task.getEvidenciaRequerida().isBlank()) {
                    sb.append(String.format("   _Evidencia:_ %s\n", task.getEvidenciaRequerida()));
                }
                sb.append("\n");
            }

            sb.append("📸 *¿Listo para registrar tu avance?*\n");
            sb.append("Cuando realicen sus microacciones, ingresen aquí para subir una foto o nota rápida en su bitácora:\n");
            sb.append(String.format("👉 https://integrity.family/dashboard/family/%d\n\n", family.getId()));
            sb.append("¡Pequeños hábitos diarios construyen hogares extraordinarios! ♥️✨");

            try {
                whatsappService.sendToFamily(family, sb.toString());
                log.info("📧 [HABIT-REMINDER] Recordatorio de hábitos enviado a familia ID: {}", family.getId());
            } catch (Exception e) {
                log.error("❌ [HABIT-REMINDER] Error al enviar WhatsApp a familia {}: {}", family.getId(), e.getMessage());
            }
        });
    }
}
