package com.integrityfamily.dto.home;

import java.util.UUID;

public record ActiveSprintBlock(
    SprintDisplayStatus status,
    UUID sprintId,
    String title,
    ProgressBlock progress,
    Command todayMission
) {
    public ActiveSprintBlock {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (sprintId == null) {
            throw new IllegalArgumentException("sprintId is required");
        }
        if (title == null) {
            throw new IllegalArgumentException("title is required");
        }
        if (progress == null) {
            throw new IllegalArgumentException("progress is required");
        }
        
        if (status == SprintDisplayStatus.ACTIVE) {
            if (todayMission == null) {
                throw new IllegalArgumentException("todayMission is required when status is ACTIVE");
            }
        } else {
            if (todayMission != null) {
                throw new IllegalArgumentException("todayMission must be null when status is not ACTIVE");
            }
        }
    }
}
