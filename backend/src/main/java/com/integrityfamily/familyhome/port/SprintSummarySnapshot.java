package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.SprintDisplayStatus;
import java.util.UUID;

public record SprintSummarySnapshot(
    UUID sprintId,
    SprintDisplayStatus status,
    String title,
    int completedMissions,
    int totalMissions,
    String todayMissionId,
    String todayMissionLabel,
    String todayMissionTarget
) {
}
