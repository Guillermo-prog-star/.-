package com.integrityfamily.hud.dto;

import com.integrityfamily.dto.home.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProfessionalHudView(
    String contractVersion,
    HudType hudType,
    ViewerContext viewerContext,
    UUID familyId,
    List<String> navigation,
    String icafStatus,
    String adaptiveCapacity,
    String riskAssessment,
    TodayBlock today,
    ActiveSprintBlock activeSprint,
    Map<String, DimensionBlock> dimensions,
    List<String> clinicalNotes,
    List<String> activeInterventions,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata
) implements AdaptiveHudView {}
