package com.integrityfamily.hud.policy;

import com.integrityfamily.hud.dto.HudModule;
import com.integrityfamily.hud.dto.HudType;
import java.util.Set;

public class HudModulePolicy {

    public static boolean isModuleAllowed(HudType hudType, HudModule module, Set<String> viewerPermissions) {
        if (hudType == HudType.FAMILY) {
            // Strictly forbid professional/clinical modules in Family HUD
            return switch (module) {
                case PRIMARY_STATUS, ACTIVE_SPRINT, DIMENSION_SUMMARY, TIMELINE, MEMORY, IDENTITY, ASSISTANT -> true;
                default -> false;
            };
        }

        if (hudType == HudType.PROFESSIONAL) {
            // Allow professional modules to authorized viewers
            return switch (module) {
                case PRIMARY_STATUS, ACTIVE_SPRINT, DIMENSION_SUMMARY, TIMELINE, ALERTS, NOTES, INTERVENTIONS, ASSESSMENT -> true;
                default -> false;
            };
        }

        return false;
    }
}
