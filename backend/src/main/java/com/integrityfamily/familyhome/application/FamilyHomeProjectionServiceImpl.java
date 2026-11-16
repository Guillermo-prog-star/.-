package com.integrityfamily.familyhome.application;

import com.integrityfamily.dto.home.*;
import com.integrityfamily.familyhome.application.exception.*;
import com.integrityfamily.familyhome.port.*;
import com.integrityfamily.familyhome.resolver.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FamilyHomeProjectionServiceImpl implements FamilyHomeProjectionService {

    private final FamilyMembershipQueryPort membershipPort;
    private final FamilyJourneyQueryPort journeyPort;
    private final FamilyProfileQueryPort profilePort;
    private final FamilyHomeViewResolverRegistry resolverRegistry;

    public FamilyHomeProjectionServiceImpl(
            FamilyMembershipQueryPort membershipPort,
            FamilyJourneyQueryPort journeyPort,
            FamilyProfileQueryPort profilePort,
            FamilyHomeViewResolverRegistry resolverRegistry) {
        this.membershipPort = membershipPort;
        this.journeyPort = journeyPort;
        this.profilePort = profilePort;
        this.resolverRegistry = resolverRegistry;
    }

    @Override
    public FamilyHomeView project(
            UUID familyId,
            UUID authenticatedUserId,
            ProjectionRequestContext requestContext) {
        
        // 1. Verify family membership
        if (!membershipPort.isMember(familyId, authenticatedUserId)) {
            throw new FamilyHomeAccessDeniedException("User " + authenticatedUserId + " is not a member of family " + familyId);
        }

        // 2. Fetch journey stage and construct viewer context
        JourneySnapshot journeySnapshot = journeyPort.getJourneyStatus(familyId);
        if (journeySnapshot == null) {
            throw new FamilyHomeDataIncompleteException("Journey status not found for family: " + familyId);
        }

        ViewerRole role = membershipPort.getRole(familyId, authenticatedUserId);
        var permissions = membershipPort.getPermissions(familyId, authenticatedUserId);
        ViewerContext viewer = new ViewerContext(authenticatedUserId, role, permissions);

        // 3. Fetch family profile
        FamilyProfileSnapshot profileSnapshot = profilePort.getProfile(familyId);
        if (profileSnapshot == null) {
            throw new FamilyHomeDataIncompleteException("Profile details not found for family: " + familyId);
        }

        // 4. Build context and resolve the polymorphic projection
        FamilyHomeProjectionContext context = new FamilyHomeProjectionContext(
            familyId,
            authenticatedUserId,
            viewer,
            journeySnapshot.stage(),
            requestContext,
            profileSnapshot
        );

        FamilyHomeViewResolver resolver = resolverRegistry.getResolver(journeySnapshot.stage());
        return resolver.resolve(context);
    }
}
