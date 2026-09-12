package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyJourneyQueryPort {
    JourneySnapshot getJourneyStatus(UUID familyId);
}
