package com.integrityfamily.dto.home;

public record OnboardingHomeView(
    String contractVersion,
    ViewType viewType,
    ViewerContext viewer,
    FamilyContext family,
    JourneyContext journey,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata,
    ModuleAvailability moduleAvailability
) implements FamilyHomeView {
    public OnboardingHomeView {
        if (!"0.9.0-Candidate".equals(contractVersion)) {
            throw new IllegalArgumentException("Unsupported contract version");
        }
        if (viewType != ViewType.ONBOARDING) {
            throw new IllegalArgumentException("Invalid viewType for OnboardingHomeView");
        }
        if (journey != null && journey.stage() != JourneyStage.NEW_FAMILY && journey.stage() != JourneyStage.PROFILE_IN_PROGRESS) {
            throw new IllegalArgumentException("Invalid journey stage for OnboardingHomeView: " + journey.stage());
        }
    }
}
