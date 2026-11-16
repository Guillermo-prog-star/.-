package com.integrityfamily.dto.home;

import java.util.Map;

public record ActiveHomeView(
    String contractVersion,
    ViewType viewType,
    ViewerContext viewer,
    FamilyContext family,
    JourneyContext journey,
    SafetyPresentation safetyPresentation,
    ResponseMetadata responseMetadata,
    ModuleAvailability moduleAvailability,
    
    TodayBlock today,
    ActiveSprintBlock activeSprint,
    Map<String, DimensionBlock> dimensions,
    ResumeBlock resumeBlock
) implements FamilyHomeView {
    public ActiveHomeView {
        if (!"0.9.0-Candidate".equals(contractVersion)) {
            throw new IllegalArgumentException("Unsupported contract version");
        }
        if (viewType != ViewType.ACTIVE_HOME) {
            throw new IllegalArgumentException("Invalid viewType for ActiveHomeView");
        }
        if (journey != null && journey.stage() != JourneyStage.ACTIVE_HOME && journey.stage() != JourneyStage.RESUMING_HOME) {
            throw new IllegalArgumentException("Invalid journey stage for ActiveHomeView: " + journey.stage());
        }
        if (today == null) {
            throw new IllegalArgumentException("today block is required for ActiveHomeView");
        }
        if (dimensions == null || dimensions.isEmpty()) {
            throw new IllegalArgumentException("dimensions are required for ActiveHomeView");
        }
        
        if (journey != null) {
            if (journey.stage() == JourneyStage.ACTIVE_HOME) {
                if (activeSprint == null) {
                    throw new IllegalArgumentException("activeSprint is required when journey stage is ACTIVE_HOME");
                }
            } else if (journey.stage() == JourneyStage.RESUMING_HOME) {
                if (resumeBlock == null) {
                    throw new IllegalArgumentException("resumeBlock is required when journey stage is RESUMING_HOME");
                }
            }
        }
    }
}
