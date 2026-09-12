package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import com.integrityfamily.familyhome.port.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class AssessmentHomeViewResolver implements FamilyHomeViewResolver {

    private final FamilyAssessmentQueryPort assessmentPort;

    public AssessmentHomeViewResolver(FamilyAssessmentQueryPort assessmentPort) {
        this.assessmentPort = assessmentPort;
    }

    @Override
    public Set<JourneyStage> supportedStages() {
        return Set.of(JourneyStage.ASSESSMENT_IN_PROGRESS, JourneyStage.ASSESSMENT_COMPLETED);
    }

    @Override
    public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
        AssessmentProgressSnapshot progress = assessmentPort.getProgress(context.familyId());

        FamilyContext familyCtx = new FamilyContext(
            context.familyProfile().id(),
            context.familyProfile().displayName(),
            null
        );

        JourneyContext journeyCtx = new JourneyContext(
            context.journeyStage(),
            new ProgressBlock(progress.completedQuestions(), progress.totalQuestions(), 
                (int) Math.round((double) progress.completedQuestions() / progress.totalQuestions() * 100)),
            new Command(
                "cmd-assess-cont",
                "Continuar evaluación",
                CommandType.NAVIGATE,
                "/journey/assessment",
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
            ModuleStatus.INSUFFICIENT_DATA,
            ModuleStatus.NOT_APPLICABLE
        );

        return new AssessmentHomeView(
            "0.9.0-Candidate",
            ViewType.ASSESSMENT,
            context.viewer(),
            familyCtx,
            journeyCtx,
            safety,
            meta,
            modules
        );
    }
}
