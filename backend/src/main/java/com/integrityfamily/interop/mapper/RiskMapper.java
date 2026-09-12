package com.integrityfamily.interop.mapper;

import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.interop.canonical.Risk;

/** {@code FamilyRiskTrajectory} (+ su {@code RiskTrajectory} del banco) → {@link Risk}. */
public final class RiskMapper {

    private RiskMapper() {}

    public static Risk toCanonical(FamilyRiskTrajectory active) {
        var bankItem = active.getTrajectory();
        return Risk.builder()
                .canonicalId("family-risk-trajectory-" + active.getId())
                .subjectId("family-" + active.getFamily().getId())
                .code(bankItem.getCode())
                .display(bankItem.getName())
                .macrodomain(bankItem.getMacrodomain() != null ? bankItem.getMacrodomain().name() : null)
                .severity(bankItem.getSeverityDefault())
                .requiresSafetyProtocol(Boolean.TRUE.equals(bankItem.getRequiresSafetyProtocol()))
                .detectedAt(active.getDetectedAt())
                .resolvedAt(active.getResolvedAt())
                .status(active.getStatus() != null ? active.getStatus().name() : null)
                .build();
    }
}
