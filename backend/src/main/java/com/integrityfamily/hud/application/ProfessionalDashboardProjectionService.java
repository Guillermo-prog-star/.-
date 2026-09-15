package com.integrityfamily.hud.application;

import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.hud.dto.professional.ProfessionalDashboardView;
import java.util.UUID;

public interface ProfessionalDashboardProjectionService {
    ProfessionalDashboardView project(
        UUID familyId,
        UUID professionalUserId,
        ProjectionRequestContext requestContext
    );
}
