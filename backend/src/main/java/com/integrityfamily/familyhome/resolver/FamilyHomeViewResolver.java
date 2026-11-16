package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.FamilyHomeView;
import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.familyhome.application.FamilyHomeProjectionContext;
import java.util.Set;

public interface FamilyHomeViewResolver {
    Set<JourneyStage> supportedStages();
    FamilyHomeView resolve(FamilyHomeProjectionContext context);
}
