package com.integrityfamily.familyhome.application;

import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.dto.home.ViewerContext;
import com.integrityfamily.familyhome.port.FamilyProfileSnapshot;
import java.util.UUID;

public record FamilyHomeProjectionContext(
    UUID familyId,
    UUID userId,
    ViewerContext viewer,
    JourneyStage journeyStage,
    ProjectionRequestContext request,
    FamilyProfileSnapshot familyProfile
) {
    public FamilyHomeProjectionContext {
        if (familyId == null) {
            throw new IllegalArgumentException("familyId is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (viewer == null) {
            throw new IllegalArgumentException("viewer is required");
        }
        if (journeyStage == null) {
            throw new IllegalArgumentException("journeyStage is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request context is required");
        }
        if (familyProfile == null) {
            throw new IllegalArgumentException("familyProfile is required");
        }
    }
}
