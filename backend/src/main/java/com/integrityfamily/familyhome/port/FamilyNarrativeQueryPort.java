package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyNarrativeQueryPort {
    NarrativeCandidate getTodayNarrative(UUID familyId);
}
