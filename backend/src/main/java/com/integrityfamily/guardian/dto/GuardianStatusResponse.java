package com.integrityfamily.guardian.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GuardianStatusResponse(
    Long familyId,
    boolean hasGuardian,
    Long guardianMemberId,
    String guardianFullName,
    LocalDateTime guardianSince,
    int totalVotes,
    List<VoteCount> voteCounts,
    boolean currentUserHasVoted,
    MissionDto activeMission,
    int participationScore,
    int completedMissions
) {
    @Builder
    public record VoteCount(Long memberId, String fullName, int votes) {}
}
