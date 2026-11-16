package com.integrityfamily.familyhome.policy;

import com.integrityfamily.dto.home.DataStatus;
import com.integrityfamily.dto.home.DisplayPace;
import java.time.Instant;

public class FamilyPresentationPolicy {
    
    public static DisplayPace determineDisplayPace(String technicalPace) {
        if (technicalPace == null) {
            return DisplayPace.BALANCED;
        }
        return switch (technicalPace) {
            case "LIGHT", "REDUCE_LOAD" -> DisplayPace.LIGHT;
            case "ACTIVE" -> DisplayPace.ACTIVE;
            case "PAUSED" -> DisplayPace.PAUSED;
            case "RESUMING" -> DisplayPace.RETURNING;
            default -> DisplayPace.BALANCED;
        };
    }
    
    public static DataStatus determineDataStatus(Instant lastUpdated, Instant now) {
        if (lastUpdated == null) {
            return DataStatus.DEGRADED;
        }
        if (lastUpdated.isBefore(now.minusSeconds(86400))) {
            return DataStatus.STALE;
        }
        return DataStatus.FRESH;
    }
}
