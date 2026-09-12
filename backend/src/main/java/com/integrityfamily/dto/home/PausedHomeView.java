package com.integrityfamily.dto.home;

public record PausedHomeView(
    String contractVersion,
    ViewType viewType,
    ViewerContext viewer,
    FamilyContext family,
    JourneyContext journey,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata,
    ModuleAvailability moduleAvailability,
    
    DisplayPace displayPace
) implements FamilyHomeView {
    public PausedHomeView {
        if (!"0.9.0-Candidate".equals(contractVersion)) {
            throw new IllegalArgumentException("Unsupported contract version");
        }
        if (viewType != ViewType.PAUSED_HOME) {
            throw new IllegalArgumentException("Invalid viewType for PausedHomeView");
        }
        if (journey != null && journey.stage() != JourneyStage.PAUSED_HOME) {
            throw new IllegalArgumentException("Invalid journey stage for PausedHomeView: " + journey.stage());
        }
        if (displayPace != DisplayPace.PAUSED) {
            throw new IllegalArgumentException("displayPace must be PAUSED for PausedHomeView");
        }
    }
}
