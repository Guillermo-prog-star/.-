package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import com.integrityfamily.familyhome.port.FamilyProfileQueryPort;
import com.integrityfamily.familyhome.port.FamilyProfileSnapshot;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OnboardingHomeViewResolver implements FamilyHomeViewResolver {

    private final FamilyProfileQueryPort profilePort;

    public OnboardingHomeViewResolver(FamilyProfileQueryPort profilePort) {
        this.profilePort = profilePort;
    }

    @Override
    public Set<JourneyStage> supportedStages() {
        return Set.of(JourneyStage.NEW_FAMILY, JourneyStage.PROFILE_IN_PROGRESS);
    }

    @Override
    public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
        FamilyProfileSnapshot profile = profilePort.getProfile(context.familyId());
        
        MediaAsset image = null;
        if (profile.featuredImageId() != null) {
            image = new MediaAsset(
                profile.featuredImageId(),
                profile.featuredImageUrl(),
                "Foto Familiar",
                null,
                MediaProcessingStatus.PROCESSED,
                ConsentStatus.GRANTED,
                MediaVisibility.FAMILY_SHARED,
                null
            );
        }

        FamilyContext familyCtx = new FamilyContext(
            profile.id(),
            profile.displayName(),
            image
        );

        JourneyContext journeyCtx = new JourneyContext(
            context.journeyStage(),
            new ProgressBlock(0, 4, 0),
            new Command(
                "cmd-onb-start",
                "Continuar",
                CommandType.NAVIGATE,
                "/onboarding/members",
                true,
                false,
                null,
                null,
                null
            )
        );

        SafetyPresentation safety = new SafetyPresentation(
            SafetyMode.NONE,
            null,
            null,
            List.of()
        );

        ResponseMetadata meta = new ResponseMetadata(
            context.request().requestedAt(),
            context.request().requestedAt().plusSeconds(3600),
            context.request().requestedContractVersion(),
            DataStatus.FRESH,
            context.request().correlationId().toString()
        );

        ModuleAvailability modules = new ModuleAvailability(
            ModuleStatus.AVAILABLE,
            ModuleStatus.NOT_APPLICABLE,
            ModuleStatus.NOT_APPLICABLE
        );

        return new OnboardingHomeView(
            "0.9.0-Candidate",
            ViewType.ONBOARDING,
            context.viewer(),
            familyCtx,
            journeyCtx,
            safety,
            meta,
            modules
        );
    }
}
