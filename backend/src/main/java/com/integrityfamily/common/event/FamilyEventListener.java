package com.integrityfamily.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener del Event Bus Familiar — Trazabilidad y auditoría del sistema vivo.
 *
 * Este componente centraliza el logging y auditoría de todos los eventos.
 * La reacción sistémica (actualización de estado longitudinal, inferencia causal)
 * está delegada a LongitudinalStateService para mantener la separación de responsabilidades.
 *
 * Principio arquitectónico: los módulos NO se llaman directamente entre sí.
 * Comunican a través del Event Bus, garantizando:
 *   - Desacoplamiento modular
 *   - Trazabilidad longitudinal
 *   - Reactividad sistémica
 *   - Consistencia eventual
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FamilyEventListener {

    /**
     * Reacciona a crisis familiar → cascada sistémica.
     *
     * La crisis NO es un módulo aislado (FALLA 3 evitada).
     * Impacta: ICF + planes + prioridad IA + alertas + seguimiento.
     */
    @Async
    @EventListener
    public void onCrisisTriggered(FamilyCrisisEvent event) {
        log.error("🆘 [EVENT-BUS] {} | Familia: {} | Categoría: {} | Emoción: {}",
                EventTopics.CRISIS_TRIGGERED,
                event.familyId(),
                event.category(),
                event.emotion());

        // La cascada ya fue ejecutada sincrónicamente en CrisisServiceImpl.
        // Este listener es el punto de extensión para reacciones adicionales:
        // - Notificación a analytics
        // - Registro en estado longitudinal
        // - Escalado de prioridad AI a CRISIS_MODE
        log.warn("[EVENT-BUS] Crisis registrada en el sistema longitudinal. Familia: {} | CriticalDay: {}",
                event.familyId(), event.criticalDayId());
    }

    /**
     * Reacciona a recálculo de ICF → notifica módulos dependientes.
     *
     * Cuando el ICF cambia (por crisis, evaluación o bitácora),
     * el Dashboard, Portal y Consultor IA deben actualizarse.
     */
    @Async
    @EventListener
    public void onIcfRecalculated(FamilyIcfRecalculatedEvent event) {
        String prevIcfStr = String.format("%.1f", event.previousIcf());
        String newIcfStr  = String.format("%.1f", event.newIcf());
        if (event.riskEscalated()) {
            log.warn("📈 [EVENT-BUS] {} | Familia: {} | ICF: {} → {} | Riesgo ESCALÓ: {} → {} | Trigger: {}",
                    EventTopics.ICF_RECALCULATED, event.familyId(),
                    prevIcfStr, newIcfStr,
                    event.previousRiskLevel(), event.newRiskLevel(), event.trigger());
        } else if (event.riskImproved()) {
            log.info("📉 [EVENT-BUS] {} | Familia: {} | ICF: {} → {} | Riesgo MEJORÓ: {} → {} | Trigger: {}",
                    EventTopics.ICF_RECALCULATED, event.familyId(),
                    prevIcfStr, newIcfStr,
                    event.previousRiskLevel(), event.newRiskLevel(), event.trigger());
        } else {
            log.info("📊 [EVENT-BUS] {} | Familia: {} | ICF: {} → {} | Trigger: {}",
                    EventTopics.ICF_RECALCULATED, event.familyId(),
                    prevIcfStr, newIcfStr, event.trigger());
        }
    }

    /**
     * Reacciona a entrada de bitácora → cierra el bucle causal.
     *
     * La bitácora ES el timeline emocional versionado.
     * Cada entrada con deterioro (moodAfter ≤ 2) debe alertar al Motor Inferencial.
     * Cada entrada con mejora (moodAfter ≥ 4) actualiza la evolución longitudinal.
     *
     * Bucle causal:
     *   Emoción → Bitácora → Event → Riesgo → Plan → Hábitos → Convivencia → ICF → Sistema
     */
    @Async
    @EventListener
    public void onJournalEntryAdded(FamilyJournalEntryEvent event) {
        if (event.indicatesDeterioration()) {
            log.warn("⚠️ [EVENT-BUS] {} | Familia: {} | Dimensión: {} | Mood: {}/5 → DETERIORO EMOCIONAL detectado",
                    EventTopics.JOURNAL_ENTRY_ADDED,
                    event.familyId(),
                    event.riskDimension(),
                    event.moodAfter());

            if (event.isCommunicationRelated()) {
                log.warn("🔴 [EVENT-BUS] {} | Familia: {}",
                        EventTopics.COMMUNICATION_COLLAPSE, event.familyId());
            }

            // EXTENSIÓN: aquí se puede llamar al Motor Inferencial para re-evaluar riesgo
            // cuando el moodAfter bajo es recurrente (patrón = n entradas negativas en 7 días)

        } else if (event.indicatesImprovement()) {
            log.info("✅ [EVENT-BUS] {} | Familia: {} | Dimensión: {} | Mood: {}/5 → MEJORA EMOCIONAL detectada",
                    EventTopics.EMOTIONAL_IMPROVEMENT,
                    event.familyId(),
                    event.riskDimension(),
                    event.moodAfter());
        } else {
            log.info("📖 [EVENT-BUS] {} | Familia: {} | Dimensión: {} | Mood: {}/5",
                    EventTopics.JOURNAL_ENTRY_ADDED,
                    event.familyId(),
                    event.riskDimension(),
                    event.moodAfter());
        }
    }
}
