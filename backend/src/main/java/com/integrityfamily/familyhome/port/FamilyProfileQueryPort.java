package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyProfileQueryPort {
    FamilyProfileSnapshot getProfile(UUID familyId);
}
