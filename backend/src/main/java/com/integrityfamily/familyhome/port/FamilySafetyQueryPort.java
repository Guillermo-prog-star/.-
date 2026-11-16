package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilySafetyQueryPort {
    SafetyPresentationCandidate getSafetyAlert(UUID familyId);
}
