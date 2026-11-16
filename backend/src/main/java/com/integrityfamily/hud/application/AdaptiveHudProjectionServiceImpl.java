package com.integrityfamily.hud.application;

import com.integrityfamily.familyhome.application.ProjectionRequestContext;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAccessDeniedException;
import com.integrityfamily.hud.dto.AdaptiveHudView;
import com.integrityfamily.hud.dto.HudType;
import com.integrityfamily.hud.permissions.HudAuthorizationPolicy;
import com.integrityfamily.hud.resolver.FamilyHudProjectionResolver;
import com.integrityfamily.hud.resolver.ProfessionalHudProjectionResolver;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AdaptiveHudProjectionServiceImpl implements AdaptiveHudProjectionService {

    private final FamilyHudProjectionResolver familyResolver;
    private final ProfessionalHudProjectionResolver professionalResolver;

    public AdaptiveHudProjectionServiceImpl(
            FamilyHudProjectionResolver familyResolver,
            ProfessionalHudProjectionResolver professionalResolver) {
        this.familyResolver = familyResolver;
        this.professionalResolver = professionalResolver;
    }

    @Override
    public AdaptiveHudView project(
            UUID familyId,
            UUID authenticatedUserId,
            ProjectionRequestContext requestContext,
            HudType hudType) {

        AdaptiveHudView view;
        if (hudType == HudType.PROFESSIONAL) {
            view = professionalResolver.resolve(familyId, authenticatedUserId, requestContext);
        } else {
            view = familyResolver.resolve(familyId, authenticatedUserId, requestContext);
        }

        // Severe server-side authorization check against user roles/permissions
        if (!HudAuthorizationPolicy.authorize(familyId, view.viewerContext(), hudType)) {
            throw new FamilyHomeAccessDeniedException("User " + authenticatedUserId + " is not authorized to access HUD of type " + hudType);
        }

        return view;
    }
}
