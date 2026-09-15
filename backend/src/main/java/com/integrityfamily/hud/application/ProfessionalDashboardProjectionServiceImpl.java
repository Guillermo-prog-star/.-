package com.integrityfamily.hud.application;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.familyhome.port.*;
import com.integrityfamily.hud.dto.professional.ProfessionalDashboardView;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class ProfessionalDashboardProjectionServiceImpl implements ProfessionalDashboardProjectionService {

    private final FamilyMembershipQueryPort membershipPort;
    private final FamilyInterventionQueryPort interventionPort;

    public ProfessionalDashboardProjectionServiceImpl(
            FamilyMembershipQueryPort membershipPort,
            FamilyInterventionQueryPort interventionPort) {
        this.membershipPort = membershipPort;
        this.interventionPort = interventionPort;
    }

    @Override
    public ProfessionalDashboardView project(
            UUID familyId,
            UUID professionalUserId,
            ProjectionRequestContext requestContext) {

        ViewerRole role = membershipPort.getRole(familyId, professionalUserId);
        List<String> permissions = membershipPort.getPermissions(familyId, professionalUserId);
        ViewerContext viewer = new ViewerContext(professionalUserId, role, permissions);

        // Clinical notes and interventions
        List<String> notes = List.of("Nota Clínica: Se observa retroceso en adherencia del sprint.");
        List<String> interventions = List.of("Intervención activa: Refuerzo comunicativo familiar semanal.");

        // Read relational dimensions (renamed from homeostasis)
        Instant now = Instant.now();
        DimensionBlock emotions = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.emotions.stable", now, false);
        DimensionBlock comms = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.communication.stable", now, false);
        DimensionBlock habits = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.habits.stable", now, false);
        DimensionBlock time = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.sharedTime.stable", now, false);
        Map<String, DimensionBlock> dimensionsMap = Map.of(
            "emotions", emotions,
            "communication", comms,
            "habits", habits,
            "sharedTime", time
        );

        return new ProfessionalDashboardView(
            familyId,
            viewer,
            63,            // ICaF Value
            "DESCENDENTE", // Trend
            0.61,          // Adaptive capacity
            "MODERADO",    // Risk level
            2,             // Alerts count
            dimensionsMap,
            notes,
            interventions
        );
    }
}
