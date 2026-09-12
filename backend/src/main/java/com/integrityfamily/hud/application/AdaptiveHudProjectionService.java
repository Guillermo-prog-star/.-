package com.integrityfamily.hud.application;

import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.hud.dto.AdaptiveHudView;
import com.integrityfamily.hud.dto.HudType;
import java.util.UUID;

public interface AdaptiveHudProjectionService {
    AdaptiveHudView project(
        UUID familyId,
        UUID authenticatedUserId,
        ProjectionRequestContext requestContext,
        HudType hudType
    );
}
