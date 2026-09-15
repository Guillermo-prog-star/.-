package com.integrityfamily.hud.dto;

import com.integrityfamily.dto.home.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FamilyHudView(
    String contractVersion,
    HudType hudType,
    ViewerContext viewerContext,
    UUID familyId,
    List<String> navigation,
    String statusMessage,
    String paceMessage,
    String communicationMessage,
    TodayBlock today,
    ActiveSprintBlock activeSprint,
    Map<String, DimensionBlock> dimensions,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata
) implements AdaptiveHudView {}
