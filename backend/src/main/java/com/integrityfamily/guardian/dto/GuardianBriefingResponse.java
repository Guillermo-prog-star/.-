package com.integrityfamily.guardian.dto;

import java.util.List;

public record GuardianBriefingResponse(
        String guardianName,
        String fatigueSignal,
        int activeParticipants,
        int inactiveParticipants,
        List<MemberSummary> members,
        String currentMilestone,
        double planCompletionRate,
        String aiMessage
) {
    public record MemberSummary(
            Long memberId,
            String name,
            boolean activeThisWeek,
            long daysSinceLastActivity
    ) {}
}
