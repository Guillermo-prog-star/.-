package com.integrityfamily.evaluation.service.post;

import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.Evaluation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Notifica a la familia que el diagnóstico está listo. */
@Component
@Order(100)
@RequiredArgsConstructor
public class NotificationStep implements PostFinalizationStep {

    private final UserNotificationService userNotificationService;

    @Override
    public String name() { return "EVALUATION_NOTIFICATION"; }

    @Override
    public void execute(PostFinalizationContext ctx) {
        Evaluation saved = ctx.evaluation();
        String riskLevel = ctx.riskLevel();
        String riskLabel = switch (riskLevel) {
            case "BAJO"     -> "Bajo ✅";
            case "MODERADO" -> "Moderado ⚠️";
            case "ALTO"     -> "Alto 🔶";
            case "CRITICO"  -> "Crítico 🔴";
            default         -> riskLevel;
        };
        userNotificationService.push(
                saved.getFamily(), null,
                "EVALUATION_DONE",
                "Diagnóstico completado 📊",
                String.format("ICF: %.1f | Riesgo: %s | Hito: %s. Revisa tu evolución en el panel clínico.",
                        saved.getIcf() != null ? saved.getIcf() : 0.0,
                        riskLabel,
                        saved.getMilestoneKey() != null ? saved.getMilestoneKey() : "—")
        );
    }
}
