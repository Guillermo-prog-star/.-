package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyInterventionQueryPort {
    SprintSummarySnapshot getActiveSprint(UUID familyId);
}
