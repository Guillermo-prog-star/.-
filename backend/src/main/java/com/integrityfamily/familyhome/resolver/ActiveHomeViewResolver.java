package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import com.integrityfamily.familyhome.policy.*;
import com.integrityfamily.familyhome.port.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class ActiveHomeViewResolver implements FamilyHomeViewResolver {

    private final FamilyInterventionQueryPort interventionPort;
    private final FamilyNarrativeQueryPort narrativePort;
    private final FamilySafetyQueryPort safetyPort;
    private final FamilyMemoryQueryPort memoryPort;

    public ActiveHomeViewResolver(
            FamilyInterventionQueryPort interventionPort,
            FamilyNarrativeQueryPort narrativePort,
            FamilySafetyQueryPort safetyPort,
            FamilyMemoryQueryPort memoryPort) {
        this.interventionPort = interventionPort;
        this.narrativePort = narrativePort;
        this.safetyPort = safetyPort;
        this.memoryPort = memoryPort;
    }

    @Override
    public Set<JourneyStage> supportedStages() {
        return Set.of(JourneyStage.ACTIVE_HOME, JourneyStage.RESUMING_HOME);
    }

    @Override
    public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
        NarrativeCandidate narrativeCandidate = narrativePort.getTodayNarrative(context.familyId());
        SprintSummarySnapshot sprintSnapshot = interventionPort.getActiveSprint(context.familyId());
        SafetyPresentationCandidate safetyCandidate = safetyPort.getSafetyAlert(context.familyId());

        NarrativeBlock narrativeBlock = null;
        if (EvidencePolicyGate.isAllowed(narrativeCandidate)) {
            NarrativeProvenance provenance = new NarrativeProvenance(
                narrativeCandidate.generatorType(),
                narrativeCandidate.generatorId(),
                narrativeCandidate.promptVersion(),
                narrativeCandidate.sourceIds(),
                narrativeCandidate.reviewStatus(),
                EvidencePolicy.FAMILY_APPROVED,
                narrativeCandidate.generatedAt(),
                narrativeCandidate.validUntil(),
                narrativeCandidate.locale(),
                narrativeCandidate.editedByHuman()
            );
            
            narrativeBlock = new NarrativeBlock(narrativeCandidate.text(), provenance);
        }

        Command todayPrimaryCommand = new Command(
            "cmd-primary-active",
            "Misión de Hoy",
            CommandType.NAVIGATE,
            "/sprint/mission",
            true,
            false,
            null,
            null,
            null
        );

        TodayBlock todayBlock = null;
        if (narrativeBlock != null) {
            todayBlock = new TodayBlock(narrativeBlock, todayPrimaryCommand, DisplayPace.BALANCED);
        } else {
            NarrativeProvenance provFallback = new NarrativeProvenance(
                GeneratorType.RULE_ENGINE,
                "fallback-narrative",
                null,
                List.of(),
                ReviewStatus.AUTO_APPROVED,
                EvidencePolicy.FAMILY_APPROVED,
                Instant.now(),
                null,
                "es",
                false
            );
            todayBlock = new TodayBlock(
                new NarrativeBlock("Mantente al día con tus actividades compartidas.", provFallback),
                todayPrimaryCommand,
                DisplayPace.BALANCED
            );
        }

        ActiveSprintBlock activeSprint = null;
        if (sprintSnapshot != null && context.journeyStage() == JourneyStage.ACTIVE_HOME) {
            Command missionCmd = null;
            if (sprintSnapshot.todayMissionId() != null) {
                missionCmd = new Command(
                    sprintSnapshot.todayMissionId(),
                    sprintSnapshot.todayMissionLabel(),
                    CommandType.NAVIGATE,
                    sprintSnapshot.todayMissionTarget(),
                    true,
                    false,
                    null,
                    null,
                    null
                );
            }
            activeSprint = new ActiveSprintBlock(
                sprintSnapshot.status(),
                sprintSnapshot.sprintId(),
                sprintSnapshot.title(),
                new ProgressBlock(sprintSnapshot.completedMissions(), sprintSnapshot.totalMissions(),
                    (int) Math.round((double) sprintSnapshot.completedMissions() / sprintSnapshot.totalMissions() * 100)),
                missionCmd
            );
        }

        ResumeBlock resumeBlock = null;
        if (context.journeyStage() == JourneyStage.RESUMING_HOME) {
            resumeBlock = new ResumeBlock(
                "family.resume.welcome",
                new Command(
                    "cmd-confirm-resume",
                    "Retomar Actividades",
                    CommandType.SUBMIT_ACTION,
                    "confirm-resume",
                    true,
                    false,
                    null,
                    null,
                    null
                )
            );
        }

        Instant now = Instant.now();
        DimensionBlock emotions = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.emotions.stable", now, false);
        DimensionBlock comms = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.communication.stable", now, false);
        DimensionBlock habits = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.habits.stable", now, false);
        DimensionBlock time = new DimensionBlock(DimensionStatus.STABLE, "family.dimension.sharedTime.stable", now, false);
        Map<String, DimensionBlock> dimensionsMap = Map.of(
            "emotions", emotions,
            "communication", comms,
            "habits", habits,
            "sharedTime", time
        );

        SafetyPresentation safety = new SafetyPresentation(
            safetyCandidate != null ? safetyCandidate.mode() : SafetyMode.NONE,
            safetyCandidate != null ? safetyCandidate.title() : null,
            safetyCandidate != null ? safetyCandidate.message() : null,
            List.of()
        );

        FamilyContext familyCtx = new FamilyContext(
            context.familyProfile().id(),
            context.familyProfile().displayName(),
            null
        );

        JourneyContext journeyCtx = new JourneyContext(
            context.journeyStage(),
            new ProgressBlock(1, 1, 100),
            null
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

        return new ActiveHomeView(
            "0.9.0-Candidate",
            ViewType.ACTIVE_HOME,
            context.viewer(),
            familyCtx,
            journeyCtx,
            safety,
            meta,
            modules,
            todayBlock,
            activeSprint,
            dimensionsMap,
            resumeBlock
        );
    }
}
