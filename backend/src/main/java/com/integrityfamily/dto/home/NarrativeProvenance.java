package com.integrityfamily.dto.home;

import java.time.Instant;
import java.util.List;

public record NarrativeProvenance(
    GeneratorType generatorType,
    String generatorId,
    String promptVersion,
    List<String> sourceIds,
    ReviewStatus reviewStatus,
    EvidencePolicy evidencePolicy,
    Instant generatedAt,
    Instant validUntil,
    String locale,
    Boolean editedByHuman
) {
    public NarrativeProvenance {
        if (generatorType == null) {
            throw new IllegalArgumentException("generatorType is required");
        }
        if (generatorId == null) {
            throw new IllegalArgumentException("generatorId is required");
        }
        if (reviewStatus == null) {
            throw new IllegalArgumentException("reviewStatus is required");
        }
        if (evidencePolicy == null) {
            throw new IllegalArgumentException("evidencePolicy is required");
        }
        if (evidencePolicy != EvidencePolicy.FAMILY_APPROVED) {
            throw new IllegalArgumentException("evidencePolicy must be FAMILY_APPROVED for family view");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt is required");
        }
    }
}
