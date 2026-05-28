package com.integrityfamily.participation.dto;

import java.util.List;

public record ParticipationPulseResponse(
        int totalMembers,
        int activeThisWeek,
        double participationRate,
        List<MemberPulse> members,
        List<DayActivity> weeklyActivity
) {
    public record MemberPulse(
            Long memberId,
            String name,
            String initials,
            boolean activeThisWeek,
            long daysSinceLastActivity
    ) {}

    public record DayActivity(
            String dayLabel,
            int eventCount
    ) {}
}
