package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyAssessmentQueryPort {
    AssessmentProgressSnapshot getProgress(UUID familyId);
    AssessmentReturnSnapshot getReturn(UUID familyId);
}
