package com.integrityfamily.hud.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.hud.application.ProfessionalDashboardProjectionService;
import com.integrityfamily.hud.dto.ProfessionalHudView;
import com.integrityfamily.hud.dto.HudType;
import com.integrityfamily.hud.dto.professional.ProfessionalDashboardView;
import com.integrityfamily.hud.policy.ProfessionalPresentationPolicy;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class ProfessionalHudProjectionResolver {

    private final ProfessionalDashboardProjectionService dashboardService;

    public ProfessionalHudProjectionResolver(ProfessionalDashboardProjectionService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public ProfessionalHudView resolve(UUID familyId, UUID authenticatedUserId, ProjectionRequestContext requestContext) {
        ProfessionalDashboardView clinicalView = dashboardService.project(familyId, authenticatedUserId, requestContext);

        List<String> navigation = List.of("Resumen", "Evaluación", "Intervención", "Trayectoria", "Registro");

        String icafStatus = ProfessionalPresentationPolicy.formatICaF(clinicalView.icafValue(), clinicalView.trend());
        String adaptiveCapacity = ProfessionalPresentationPolicy.formatAdaptiveCapacity(clinicalView.adaptiveCapacity());
        String riskAssessment = ProfessionalPresentationPolicy.formatRisk(clinicalView.riskLevel(), clinicalView.activeAlertsCount());

        return new ProfessionalHudView(
            "1.0.0-AdaptiveHUD",
            HudType.PROFESSIONAL,
            clinicalView.viewer(),
            familyId,
            navigation,
            icafStatus,
            adaptiveCapacity,
            riskAssessment,
            null,
            null,
            clinicalView.dimensions(),
            clinicalView.clinicalNotes(),
            clinicalView.activeInterventions(),
            null, // safetyPresentation
            new ResponseMetadata(Instant.now(), Instant.now().plusSeconds(60), "1.0.0", DataStatus.FRESH, UUID.randomUUID().toString())
        );
    }
}
