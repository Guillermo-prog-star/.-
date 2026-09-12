package com.integrityfamily.familyhome.application;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.exception.*;
import com.integrityfamily.familyhome.policy.EvidencePolicyGate;
import com.integrityfamily.familyhome.port.*;
import com.integrityfamily.familyhome.resolver.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class FamilyHomeProjectionServiceTest {

    private FamilyMembershipQueryPort membershipPort;
    private final FamilyJourneyQueryPort journeyPort = Mockito.mock(FamilyJourneyQueryPort.class);
    private final FamilyProfileQueryPort profilePort = Mockito.mock(FamilyProfileQueryPort.class);
    private final FamilyAssessmentQueryPort assessmentPort = Mockito.mock(FamilyAssessmentQueryPort.class);
    private final FamilyInterventionQueryPort interventionPort = Mockito.mock(FamilyInterventionQueryPort.class);
    private final FamilyNarrativeQueryPort narrativePort = Mockito.mock(FamilyNarrativeQueryPort.class);
    private final FamilySafetyQueryPort safetyPort = Mockito.mock(FamilySafetyQueryPort.class);
    private final FamilyMemoryQueryPort memoryPort = Mockito.mock(FamilyMemoryQueryPort.class);

    private FamilyHomeProjectionService projectionService;

    // References to real resolvers to test registry errors
    private OnboardingHomeViewResolver onboardingResolver;
    private AssessmentHomeViewResolver assessmentResolver;
    private ReturnStageHomeViewResolver returnResolver;
    private ActiveHomeViewResolver activeResolver;
    private PausedHomeViewResolver pausedResolver;

    @BeforeEach
    public void setUp() {
        membershipPort = Mockito.mock(FamilyMembershipQueryPort.class);

        onboardingResolver = new OnboardingHomeViewResolver(profilePort);
        assessmentResolver = new AssessmentHomeViewResolver(assessmentPort);
        returnResolver = new ReturnStageHomeViewResolver(assessmentPort);
        activeResolver = new ActiveHomeViewResolver(interventionPort, narrativePort, safetyPort, memoryPort);
        pausedResolver = new PausedHomeViewResolver(safetyPort);

        FamilyHomeViewResolverRegistry registry = new FamilyHomeViewResolverRegistry(List.of(
            onboardingResolver, assessmentResolver, returnResolver, activeResolver, pausedResolver
        ));

        projectionService = new FamilyHomeProjectionServiceImpl(
            membershipPort,
            journeyPort,
            profilePort,
            registry
        );
    }

    @Test
    public void testRegistryRejectsDuplicateStage() {
        // Create duplicate resolver mappings
        FamilyHomeViewResolver dupResolver = new FamilyHomeViewResolver() {
            @Override
            public Set<JourneyStage> supportedStages() {
                return Set.of(JourneyStage.NEW_FAMILY); // Clashes with onboardingResolver
            }
            @Override
            public FamilyHomeView resolve(FamilyHomeProjectionContext context) {
                return null;
            }
        };

        assertThrows(IllegalStateException.class, () ->
            new FamilyHomeViewResolverRegistry(List.of(
                onboardingResolver, assessmentResolver, returnResolver, activeResolver, pausedResolver, dupResolver
            ))
        );
    }

    @Test
    public void testRegistryRejectsMissingStage() {
        // Omit paused resolver
        assertThrows(IllegalStateException.class, () ->
            new FamilyHomeViewResolverRegistry(List.of(
                onboardingResolver, assessmentResolver, returnResolver, activeResolver
            ))
        );
    }

    @ParameterizedTest
    @EnumSource(JourneyStage.class)
    public void testCoverageOfAllNineJourneyStages(JourneyStage stage) {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(membershipPort.isMember(familyId, userId)).thenReturn(true);
        when(membershipPort.getRole(familyId, userId)).thenReturn(ViewerRole.ADULT_MEMBER);
        when(membershipPort.getPermissions(familyId, userId)).thenReturn(List.of("READ"));

        when(journeyPort.getJourneyStatus(familyId)).thenReturn(new JourneySnapshot(stage, 2, 5));
        when(profilePort.getProfile(familyId)).thenReturn(new FamilyProfileSnapshot(familyId, "Lopez Family", null, null));
        when(assessmentPort.getProgress(familyId)).thenReturn(new AssessmentProgressSnapshot(1, 3));
        when(assessmentPort.getReturn(familyId)).thenReturn(new AssessmentReturnSnapshot(List.of(), "Strengths", true));
        when(interventionPort.getActiveSprint(familyId)).thenReturn(new SprintSummarySnapshot(
            UUID.randomUUID(), SprintDisplayStatus.ACTIVE, "Sprint 1", 2, 5, "m-1", "Completar Misión", "/misions/1"
        ));
        when(narrativePort.getTodayNarrative(familyId)).thenReturn(new NarrativeCandidate(
            "Hoy conversamos.", GeneratorType.RULE_ENGINE, "rule-today", null, List.of(), ReviewStatus.AUTO_APPROVED, "FAMILY_APPROVED", Instant.now(), null, "es", false
        ));
        when(safetyPort.getSafetyAlert(familyId)).thenReturn(new SafetyPresentationCandidate(SafetyMode.NONE, null, null));

        ProjectionRequestContext req = new ProjectionRequestContext(
            Locale.ENGLISH,
            ProjectionChannel.WEB,
            UUID.randomUUID(),
            Instant.now(),
            "0.9.0-Candidate"
        );

        FamilyHomeView view = projectionService.project(familyId, userId, req);
        assertNotNull(view);

        // Assert view mapping mapping JourneyStage -> ViewType
        switch (stage) {
            case NEW_FAMILY, PROFILE_IN_PROGRESS -> assertEquals(ViewType.ONBOARDING, view.viewType());
            case ASSESSMENT_IN_PROGRESS, ASSESSMENT_COMPLETED -> assertEquals(ViewType.ASSESSMENT, view.viewType());
            case RETURN_AVAILABLE, FIRST_SPRINT_PENDING -> assertEquals(ViewType.RETURN_STAGE, view.viewType());
            case ACTIVE_HOME, RESUMING_HOME -> assertEquals(ViewType.ACTIVE_HOME, view.viewType());
            case PAUSED_HOME -> assertEquals(ViewType.PAUSED_HOME, view.viewType());
        }
    }

    @Test
    public void testDenyByDefaultOnEvidencePolicyGate() {
        // Deny classifications
        assertFalse(EvidencePolicyGate.isAllowed(createNarrativeCandidate("PROFESSIONAL_ONLY")));
        assertFalse(EvidencePolicyGate.isAllowed(createNarrativeCandidate("SHADOW_ONLY")));
        assertFalse(EvidencePolicyGate.isAllowed(createNarrativeCandidate("UNKNOWN")));
        assertFalse(EvidencePolicyGate.isAllowed(createNarrativeCandidate(null)));

        // Allow classification
        assertTrue(EvidencePolicyGate.isAllowed(createNarrativeCandidate("FAMILY_APPROVED")));
    }

    private NarrativeCandidate createNarrativeCandidate(String policy) {
        return new NarrativeCandidate(
            "Texto de prueba.",
            GeneratorType.RULE_ENGINE,
            "rule-1",
            null,
            List.of(),
            ReviewStatus.AUTO_APPROVED,
            policy,
            Instant.now(),
            null,
            "es",
            false
        );
    }

    @Test
    public void testCriticalDataMissingThrowsException() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(membershipPort.isMember(familyId, userId)).thenReturn(true);

        // Journey status missing -> throw Exception
        when(journeyPort.getJourneyStatus(familyId)).thenReturn(null);

        ProjectionRequestContext req = new ProjectionRequestContext(
            Locale.ENGLISH,
            ProjectionChannel.WEB,
            UUID.randomUUID(),
            Instant.now(),
            "0.9.0-Candidate"
        );

        assertThrows(FamilyHomeDataIncompleteException.class, () ->
            projectionService.project(familyId, userId, req)
        );
    }

    @Test
    public void testActiveHomeSprintMissingThrowsException() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(membershipPort.isMember(familyId, userId)).thenReturn(true);
        when(journeyPort.getJourneyStatus(familyId)).thenReturn(new JourneySnapshot(JourneyStage.ACTIVE_HOME, 5, 5));
        when(profilePort.getProfile(familyId)).thenReturn(new FamilyProfileSnapshot(familyId, "Lopez Family", null, null));
        when(narrativePort.getTodayNarrative(familyId)).thenReturn(null);
        when(safetyPort.getSafetyAlert(familyId)).thenReturn(null);

        // ACTIVE_HOME requires active sprint DTO validation
        when(interventionPort.getActiveSprint(familyId)).thenReturn(null);

        ProjectionRequestContext req = new ProjectionRequestContext(
            Locale.ENGLISH,
            ProjectionChannel.WEB,
            UUID.randomUUID(),
            Instant.now(),
            "0.9.0-Candidate"
        );

        // DTO invariant check should fail inside ActiveHomeView constructor when activeSprint is null
        assertThrows(IllegalArgumentException.class, () ->
            projectionService.project(familyId, userId, req)
        );
    }
}
