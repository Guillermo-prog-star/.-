package com.integrityfamily.domain;

/** Origen de una observación en {@link HypothesisEvidence} (ADR-004). */
public enum EvidenceSource {
    MANUAL,
    AUTOMATIC,
    DERIVED,
    IMPORT,
    SIMULATION
}
