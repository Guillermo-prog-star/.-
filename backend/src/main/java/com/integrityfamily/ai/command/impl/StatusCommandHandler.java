package com.integrityfamily.ai.command.impl;

import com.integrityfamily.ai.command.CommandHandler;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.ImprovementPlan;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatusCommandHandler implements CommandHandler {

    @Override
    public String handle(Family family, RiskSnapshot risk, List<ImprovementPlan> plans, boolean sentinel) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 ESTADO ACTUAL: ").append(family.getName()).append("\n");
        sb.append("- **Hito Actual:** ").append(family.getCurrentMilestone()).append("\n");
        sb.append("- **Sentinel:** ").append(sentinel ? "🚨 ALERTA CRÍTICA" : "✅ NORMAL").append("\n");
        
        if (risk != null) {
            sb.append("- **Nivel de Riesgo:** ").append(risk.getLevel()).append("\n");
        }
        
        sb.append("- **Planes Activos:** ").append(plans.size()).append("\n");
        
        return sb.toString();
    }

    @Override
    public String getCommandName() {
        return "status";
    }
}
