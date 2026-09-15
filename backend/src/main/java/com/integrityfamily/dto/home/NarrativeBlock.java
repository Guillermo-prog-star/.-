package com.integrityfamily.dto.home;

public record NarrativeBlock(
    String text,
    NarrativeProvenance provenance
) {
    public NarrativeBlock {
        if (text == null) {
            throw new IllegalArgumentException("text is required");
        }
        if (provenance == null) {
            throw new IllegalArgumentException("provenance is required");
        }
    }
}
