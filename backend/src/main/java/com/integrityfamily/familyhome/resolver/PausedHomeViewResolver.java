package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import com.integrityfamily.familyhome.port.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PausedHomeViewResolver implements FamilyHomeViewResolver {

    private final FamilySafetyQueryPort safetyPort;

    public PausedHomeViewResolver(FamilySafetyQueryPort safetyPort) {
        this.safetyPort = safetyPort;
    }

    @Override
    public Set<JourneyStage> supportedStages() {
        return Set.of(JourneyStage.PAUSED_HOME);
    }

    @Override
    public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
        SafetyPresentationCandidate safetyCandidate = safetyPort.getSafetyAlert(context.familyId());

        FamilyContext familyCtx = new FamilyContext(
            context.familyProfile().id(),
            context.familyProfile().displayName(),
            null
        );

        JourneyContext journeyCtx = new JourneyContext(
            context.journeyStage(),
            new ProgressBlock(1, 1, 100),
            new Command(
                "cmd-paused-res",
                "Reanudar Proceso",
                CommandType.SUBMIT_ACTION,
                "resume-journey",
                true,
                false,
                null,
                null,
                null
            )
        );

        SafetyPresentation safety = new SafetyPresentation(
            safetyCandidate != null ? safetyCandidate.mode() : SafetyMode.NONE,
            safetyCandidate != null ? safetyCandidate.title() : null,
            safetyCandidate != null ? safetyCandidate.message() : null,
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
            ModuleStatus.DISABLED,
            ModuleStatus.DISABLED
        );

        return new PausedHomeView(
            "0.9.0-Candidate",
            ViewType.PAUSED_HOME,
            context.viewer(),
            familyCtx,
            journeyCtx,
            safety,
            meta,
            modules,
            DisplayPace.PAUSED
        );
    }
}
