package com.integrityfamily.familyhome.application;

import com.integrityfamily.dto.home.FamilyHomeView;
import java.util.UUID;

public interface FamilyHomeProjectionService {

    FamilyHomeView project(
        UUID familyId,
        UUID authenticatedUserId,
        ProjectionRequestContext requestContext
    );
}
