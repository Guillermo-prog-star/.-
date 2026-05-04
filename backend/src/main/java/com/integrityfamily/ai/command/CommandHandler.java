package com.integrityfamily.ai.command;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.ImprovementPlan;
import java.util.List;

/**
 * SDD-AI-02: Interface for AI Power Commands.
 * Refactorizada para usar el modelo de ImprovementPlan.
 */
public interface CommandHandler {
    /**
     * Executes the command logic.
     * @param family Current family context.
     * @param risk Latest risk snapshot.
     * @param plans Active plans.
     * @param sentinel Active crisis alert.
     * @return Markdown formatted response.
     */
    String handle(Family family, RiskSnapshot risk, List<ImprovementPlan> plans, boolean sentinel);

    /**
     * Returns the command trigger (e.g., "status").
     */
    String getCommandName();
}
