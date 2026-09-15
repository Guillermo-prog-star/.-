package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.ViewerRole;
import java.util.List;
import java.util.UUID;

public interface FamilyMembershipQueryPort {
    boolean isMember(UUID familyId, UUID userId);
    ViewerRole getRole(UUID familyId, UUID userId);
    List<String> getPermissions(UUID familyId, UUID userId);
}
