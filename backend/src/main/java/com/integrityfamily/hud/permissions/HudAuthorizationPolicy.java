package com.integrityfamily.hud.permissions;

import com.integrityfamily.dto.home.ViewerContext;
import com.integrityfamily.dto.home.ViewerRole;
import com.integrityfamily.hud.dto.HudType;
import java.util.UUID;

public class HudAuthorizationPolicy {

    public static boolean authorize(UUID familyId, ViewerContext viewer, HudType targetType) {
        if (viewer == null) {
            return false;
        }
        
        // Family HUD is open to all family members (parents, children, etc.)
        if (targetType == HudType.FAMILY) {
            return viewer.role() == ViewerRole.ADULT_MEMBER || viewer.role() == ViewerRole.YOUNG_MEMBER || viewer.role() == ViewerRole.SUPPORT_PERSON || viewer.permissions().contains("VIEW_FAMILY_HUD");
        }

        // Professional HUD is open to professional support members or administrators
        if (targetType == HudType.PROFESSIONAL) {
            return viewer.role() == ViewerRole.SUPPORT_PERSON || viewer.permissions().contains("VIEW_PROFESSIONAL_HUD");
        }

        return false;
    }
}
