package com.integrityfamily.dto.home;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "viewType",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OnboardingHomeView.class, name = "ONBOARDING"),
    @JsonSubTypes.Type(value = AssessmentHomeView.class, name = "ASSESSMENT"),
    @JsonSubTypes.Type(value = ReturnStageHomeView.class, name = "RETURN_STAGE"),
    @JsonSubTypes.Type(value = ActiveHomeView.class, name = "ACTIVE_HOME"),
    @JsonSubTypes.Type(value = PausedHomeView.class, name = "PAUSED_HOME")
})
public sealed interface FamilyHomeView permits OnboardingHomeView, AssessmentHomeView, ReturnStageHomeView, ActiveHomeView, PausedHomeView {
    String contractVersion();
    ViewType viewType();
    ViewerContext viewer();
    FamilyContext family();
    JourneyContext journey();
    SafetyPresentation safetyPresentation();
    ResponseMetadata responseMetadata();
    ModuleAvailability moduleAvailability();
}
