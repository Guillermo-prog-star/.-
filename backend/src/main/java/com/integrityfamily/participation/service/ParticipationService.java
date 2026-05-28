package com.integrityfamily.participation.service;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.ParticipationEvent;
import com.integrityfamily.domain.ParticipationEventType;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ParticipationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ParticipationService {

    private static final int ACTIVITY_WINDOW_DAYS = 7;
    private static final int INACTIVITY_THRESHOLD_DAYS = 10;

    private final ParticipationEventRepository repo;
    private final FamilyRepository familyRepository;

    @Transactional
    public void record(Long familyId, Long memberId, ParticipationEventType type) {
        if (familyId == null) return;
        repo.save(ParticipationEvent.builder()
                .familyId(familyId)
                .memberId(memberId)
                .eventType(type)
                .build());
        log.debug("[PARTICIPATION] {} — familia={} miembro={}", type, familyId, memberId);
    }

    @Transactional(readOnly = true)
    public FamilyParticipationSummary getSummary(Long familyId, Long guardianMemberId) {
        LocalDateTime since = LocalDateTime.now().minusDays(ACTIVITY_WINDOW_DAYS);
        List<Long> activeMemberIds = repo.findActiveMemberIds(familyId, since);

        List<FamilyMember> allActive = familyRepository.findById(familyId)
                .map(f -> f.getMembers().stream().filter(FamilyMember::isActive).toList())
                .orElse(List.of());

        int totalMembers = allActive.size();
        int activeCount = activeMemberIds.size();
        int inactiveCount = totalMembers - activeCount;

        List<MemberActivity> memberActivities = allActive.stream()
                .map(m -> {
                    LocalDateTime last = repo.findLastActivityByMember(familyId, m.getId()).orElse(null);
                    long daysSince = last == null ? 999 : ChronoUnit.DAYS.between(last, LocalDateTime.now());
                    return new MemberActivity(m.getId(), m.getFullName(), last, daysSince, activeMemberIds.contains(m.getId()));
                })
                .toList();

        String fatigue = detectFatigueSignal(guardianMemberId, activeMemberIds, totalMembers);

        return new FamilyParticipationSummary(totalMembers, activeCount, inactiveCount, memberActivities, fatigue);
    }

    /**
     * NONE   — guardián no es el único activo
     * MILD   — solo 1 otro miembro activo en familia de ≥4 personas
     * HIGH   — guardián es el único activo
     */
    private String detectFatigueSignal(Long guardianId, List<Long> activeIds, int totalMembers) {
        if (guardianId == null) return "NONE";
        List<Long> othersActive = activeIds.stream().filter(id -> !id.equals(guardianId)).toList();
        if (othersActive.isEmpty()) return "HIGH";
        if (othersActive.size() == 1 && totalMembers >= 4) return "MILD";
        return "NONE";
    }

    public record MemberActivity(
            Long memberId,
            String fullName,
            LocalDateTime lastActivity,
            long daysSinceLastActivity,
            boolean activeThisWeek
    ) {}

    public record FamilyParticipationSummary(
            int totalMembers,
            int activeParticipants,
            int inactiveMembers,
            List<MemberActivity> activities,
            String fatigueSignal
    ) {}
}
