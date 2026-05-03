package com.integrityfamily.report.dto;

import com.integrityfamily.domain.Family;
import java.util.List;

/**
 * SDD-REP-01: Data contract for executive transformation synthesis.
 * Includes Regional Comparison data for institutional benchmarking.
 */
public record TransformationSummary(
    Long familyId,
    String familyName,
    Double initialIcf,
    Double currentIcf,
    Double peakIcf,
    Double regionalAverageIcf, // + New benchmarking field
    long sentinelAlertsTriggered,
    long missionsCompleted,
    String currentMilestone,
    List<String> dimensionProgress
) {}


