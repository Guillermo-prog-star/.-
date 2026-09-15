package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.JourneyStage;

public record JourneySnapshot(
    JourneyStage stage,
    int completedTasks,
    int totalTasks
) {
}
