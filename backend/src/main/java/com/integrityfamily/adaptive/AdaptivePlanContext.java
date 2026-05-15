package com.integrityfamily.adaptive;

public record AdaptivePlanContext(
        Long familyId,
        double adherencePercent,
        int inactivityDays,
        int previousCommunicationScore,
        int currentCommunicationScore,
        double overdueTasksPercent
) {
    public AdaptivePlanContext {
        if (familyId == null || familyId <= 0) {
            throw new IllegalArgumentException("familyId must be valid.");
        }
        if (adherencePercent < 0 || adherencePercent > 100) {
            throw new IllegalArgumentException("adherencePercent must be between 0 and 100.");
        }
        if (inactivityDays < 0) {
            throw new IllegalArgumentException("inactivityDays cannot be negative.");
        }
        if (previousCommunicationScore < 0 || previousCommunicationScore > 100) {
            throw new IllegalArgumentException("previousCommunicationScore must be between 0 and 100.");
        }
        if (currentCommunicationScore < 0 || currentCommunicationScore > 100) {
            throw new IllegalArgumentException("currentCommunicationScore must be between 0 and 100.");
        }
        if (overdueTasksPercent < 0 || overdueTasksPercent > 100) {
            throw new IllegalArgumentException("overdueTasksPercent must be between 0 and 100.");
        }
    }

    public int communicationDrop() {
        return previousCommunicationScore - currentCommunicationScore;
    }
}
