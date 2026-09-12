package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import com.integrityfamily.familyhome.port.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ReturnStageHomeViewResolver implements FamilyHomeViewResolver {

    private final FamilyAssessmentQueryPort assessmentPort;

    public ReturnStageHomeViewResolver(FamilyAssessmentQueryPort assessmentPort) {
        this.assessmentPort = assessmentPort;
    }

    @Override
    public Set<JourneyStage> supportedStages() {
        return Set.of(JourneyStage.RETURN_AVAILABLE, JourneyStage.FIRST_SPRINT_PENDING);
    }

    @Override
    public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
        AssessmentReturnSnapshot returnData = assessmentPort.getReturn(context.familyId());

        FamilyContext familyCtx = new FamilyContext(
            context.familyProfile().id(),
            context.familyProfile().displayName(),
            null
        );

        JourneyContext journeyCtx = new JourneyContext(
            context.journeyStage(),
            new ProgressBlock(1, 1, 100),
            new Command(
                "cmd-ret-accept",
                "Aceptar Primer Sprint",
                CommandType.SUBMIT_ACTION,
                "accept-first-sprint",
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
            ModuleStatus.AVAILABLE,
            ModuleStatus.AVAILABLE
        );

        return new ReturnStageHomeView(
            "0.9.0-Candidate",
            ViewType.RETURN_STAGE,
            context.viewer(),
            familyCtx,
            journeyCtx,
            safety,
            meta,
            modules
        );
    }
}
