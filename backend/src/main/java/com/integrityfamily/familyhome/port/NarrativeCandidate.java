package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.GeneratorType;
import com.integrityfamily.dto.home.ReviewStatus;
import java.time.Instant;
import java.util.List;

public record NarrativeCandidate(
    String text,
    GeneratorType generatorType,
    String generatorId,
    String promptVersion,
    List<String> sourceIds,
    ReviewStatus reviewStatus,
    String rawEvidencePolicy, // Checked by EvidencePolicyGate
    Instant generatedAt,
    Instant validUntil,
    String locale,
    Boolean editedByHuman
) {
}
