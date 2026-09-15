package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyIdentityQueryPort {
    String getLema(UUID familyId);
}
