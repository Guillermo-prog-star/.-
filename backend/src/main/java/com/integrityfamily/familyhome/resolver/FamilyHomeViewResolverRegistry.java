package com.integrityfamily.familyhome.resolver;

import com.integrityfamily.dto.home.JourneyStage;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FamilyHomeViewResolverRegistry {

    private final Map<JourneyStage, FamilyHomeViewResolver> resolvers = new EnumMap<>(JourneyStage.class);

    public FamilyHomeViewResolverRegistry(List<FamilyHomeViewResolver> resolverList) {
        for (FamilyHomeViewResolver resolver : resolverList) {
            for (JourneyStage stage : resolver.supportedStages()) {
                if (resolvers.containsKey(stage)) {
                    throw new IllegalStateException("Duplicate resolver registered for stage: " + stage);
                }
                resolvers.put(stage, resolver);
            }
        }
        
        for (JourneyStage stage : JourneyStage.values()) {
            if (!resolvers.containsKey(stage)) {
                throw new IllegalStateException("Missing resolver for journey stage: " + stage);
            }
        }
    }

    public FamilyHomeViewResolver getResolver(JourneyStage stage) {
        FamilyHomeViewResolver resolver = resolvers.get(stage);
        if (resolver == null) {
            throw new IllegalArgumentException("No resolver registered for stage: " + stage);
        }
        return resolver;
    }
}
