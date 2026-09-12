package com.integrityfamily.dto.home;

public record AssessmentHomeView(
    String contractVersion,
    ViewType viewType,
    ViewerContext viewer,
    FamilyContext family,
    JourneyContext journey,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata,
    ModuleAvailability moduleAvailability
) implements FamilyHomeView {
    public AssessmentHomeView {
        if (!"0.9.0-Candidate".equals(contractVersion)) {
            throw new IllegalArgumentException("Unsupported contract version");
        }
        if (viewType != ViewType.ASSESSMENT) {
            throw new IllegalArgumentException("Invalid viewType for AssessmentHomeView");
        }
        if (journey != null && journey.stage() != JourneyStage.ASSESSMENT_IN_PROGRESS && journey.stage() != JourneyStage.ASSESSMENT_COMPLETED) {
            throw new IllegalArgumentException("Invalid journey stage for AssessmentHomeView: " + journey.stage());
        }
    }
}
