package com.integrityfamily.hud.dto.professional;

import com.integrityfamily.dto.home.DimensionBlock;
import com.integrityfamily.dto.home.ViewerContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProfessionalDashboardView(
    UUID familyId,
    ViewerContext viewer,
    int icafValue,
    String trend,
    double adaptiveCapacity,
    String riskLevel,
    int activeAlertsCount,
    Map<String, DimensionBlock> dimensions,
    List<String> clinicalNotes,
    List<String> activeInterventions
) {}
