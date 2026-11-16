package com.integrityfamily.hud.permissions;

import com.integrityfamily.dto.home.ViewerContext;
import com.integrityfamily.dto.home.ViewerRole;
import com.integrityfamily.hud.dto.HudType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class AdaptiveHudSecurityTest {

    @Test
    public void testFamilyHudAllowedForFamilyMembers() {
        UUID familyId = UUID.randomUUID();
        ViewerContext adult = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of());
        ViewerContext youth = new ViewerContext(UUID.randomUUID(), ViewerRole.YOUNG_MEMBER, List.of());
        ViewerContext professional = new ViewerContext(UUID.randomUUID(), ViewerRole.SUPPORT_PERSON, List.of());

        assertTrue(HudAuthorizationPolicy.authorize(familyId, adult, HudType.FAMILY));
        assertTrue(HudAuthorizationPolicy.authorize(familyId, youth, HudType.FAMILY));
        assertTrue(HudAuthorizationPolicy.authorize(familyId, professional, HudType.FAMILY));
    }

    @Test
    public void testProfessionalHudBlockedForRegularFamilyMembers() {
        UUID familyId = UUID.randomUUID();
        ViewerContext adult = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of());
        ViewerContext youth = new ViewerContext(UUID.randomUUID(), ViewerRole.YOUNG_MEMBER, List.of());

        // Regular family members should not have access to Professional HUD by default
        assertFalse(HudAuthorizationPolicy.authorize(familyId, adult, HudType.PROFESSIONAL));
        assertFalse(HudAuthorizationPolicy.authorize(familyId, youth, HudType.PROFESSIONAL));
    }

    @Test
    public void testProfessionalHudAllowedForAuthorizedProfessionalsOnly() {
        UUID familyId = UUID.randomUUID();
        ViewerContext professional = new ViewerContext(UUID.randomUUID(), ViewerRole.SUPPORT_PERSON, List.of());
        ViewerContext familyMemberWithSpecialPerm = new ViewerContext(UUID.randomUUID(), ViewerRole.ADULT_MEMBER, List.of("VIEW_PROFESSIONAL_HUD"));

        assertTrue(HudAuthorizationPolicy.authorize(familyId, professional, HudType.PROFESSIONAL));
        assertTrue(HudAuthorizationPolicy.authorize(familyId, familyMemberWithSpecialPerm, HudType.PROFESSIONAL));
    }
}
