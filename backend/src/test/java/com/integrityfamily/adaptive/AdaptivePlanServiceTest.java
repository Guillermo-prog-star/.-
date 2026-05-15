package com.integrityfamily.adaptive;

import com.integrityfamily.analytics.service.ConvivenceAnalyticsService;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdaptivePlanServiceTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private ImprovementPlanRepository improvementPlanRepository;
    @Mock private PlanTaskRepository planTaskRepository;
    @Mock private TaskEvidenceRepository taskEvidenceRepository;
    @Mock private FamilyMetricsSnapshotRepository familyMetricsSnapshotRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private AdaptiveAdjustmentRepository adaptiveAdjustmentRepository;
    @Mock private ConvivenceAnalyticsService convivenceAnalyticsService;

    @InjectMocks
    private AdaptivePlanService service;

    @Test
    void shouldProposeLoadReductionWhenAdherenceIsBelow40() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                39.9,
                0,
                80,
                80,
                0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertEquals(1, result.size());
        assertEquals(AdaptiveRuleType.REDUCE_LOAD, result.get(0).getRuleType());
        assertEquals(AdjustmentStatus.PROPOSED, result.get(0).getStatus());
    }

    @Test
    void shouldNotProposeLoadReductionWhenAdherenceIsExactly40() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                40.0,
                0,
                80,
                80,
                0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldProposeSoftResetWhenInactivityIsAtLeast14Days() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                80,
                14,
                80,
                80,
                0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertEquals(1, result.size());
        assertEquals(AdaptiveRuleType.SOFT_RESET, result.get(0).getRuleType());
    }

    @Test
    void shouldProposeGuidedListeningWhenCommunicationDropsMoreThan15Points() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                80,
                0,
                90,
                74,
                0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertEquals(1, result.size());
        assertEquals(AdaptiveRuleType.GUIDED_LISTENING, result.get(0).getRuleType());
    }

    @Test
    void shouldNotProposeGuidedListeningWhenCommunicationDropsExactly15Points() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                80,
                0,
                90,
                75,
                0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldProposePauseNonCriticalWhenOverdueTasksExceed50Percent() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                80,
                0,
                80,
                80,
                50.1
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertEquals(1, result.size());
        assertEquals(AdaptiveRuleType.PAUSE_NON_CRITICAL, result.get(0).getRuleType());
    }

    @Test
    void shouldNotProposePauseNonCriticalWhenOverdueTasksAreExactly50Percent() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                80,
                0,
                80,
                80,
                50.0
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldProposeAllAdjustmentsWhenAllRulesMatch() {
        AdaptivePlanContext context = new AdaptivePlanContext(
                1L,
                30,
                20,
                90,
                70,
                80
        );

        List<AdaptiveAdjustment> result = service.evaluate(context);

        assertEquals(4, result.size());
        assertTrue(result.stream().anyMatch(a -> a.getRuleType() == AdaptiveRuleType.REDUCE_LOAD));
        assertTrue(result.stream().anyMatch(a -> a.getRuleType() == AdaptiveRuleType.SOFT_RESET));
        assertTrue(result.stream().anyMatch(a -> a.getRuleType() == AdaptiveRuleType.GUIDED_LISTENING));
        assertTrue(result.stream().anyMatch(a -> a.getRuleType() == AdaptiveRuleType.PAUSE_NON_CRITICAL));
    }

    @Test
    void shouldApproveAdjustmentSuccessfully() {
        AdaptiveAdjustment adjustment = new AdaptiveAdjustment(
                1L,
                AdaptiveRuleType.REDUCE_LOAD,
                "Test adjustment"
        );

        service.approve(adjustment);

        assertEquals(AdjustmentStatus.APPROVED, adjustment.getStatus());
    }

    @Test
    void shouldApplyApprovedAdjustmentSuccessfully() {
        AdaptiveAdjustment adjustment = new AdaptiveAdjustment(
                1L,
                AdaptiveRuleType.REDUCE_LOAD,
                "Test adjustment"
        );

        service.approve(adjustment);
        service.apply(adjustment);

        assertEquals(AdjustmentStatus.APPLIED, adjustment.getStatus());
    }

    @Test
    void shouldFailWhenApplyingWithoutApproval() {
        AdaptiveAdjustment adjustment = new AdaptiveAdjustment(
                1L,
                AdaptiveRuleType.REDUCE_LOAD,
                "Test adjustment"
        );

        assertThrows(IllegalStateException.class, () -> service.apply(adjustment));
    }
}
