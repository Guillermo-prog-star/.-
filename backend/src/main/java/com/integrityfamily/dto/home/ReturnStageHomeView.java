package com.integrityfamily.dto.home;

public record ReturnStageHomeView(
    String contractVersion,
    ViewType viewType,
    ViewerContext viewer,
    FamilyContext family,
    JourneyContext journey,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata,
    ModuleAvailability moduleAvailability
) implements FamilyHomeView {
    public ReturnStageHomeView {
        if (!"0.9.0-Candidate".equals(contractVersion)) {
            throw new IllegalArgumentException("Unsupported contract version");
        }
        if (viewType != ViewType.RETURN_STAGE) {
            throw new IllegalArgumentException("Invalid viewType for ReturnStageHomeView");
        }
        if (journey != null && journey.stage() != JourneyStage.RETURN_AVAILABLE && journey.stage() != JourneyStage.FIRST_SPRINT_PENDING) {
            throw new IllegalArgumentException("Invalid journey stage for ReturnStageHomeView: " + journey.stage());
        }
    }
}
