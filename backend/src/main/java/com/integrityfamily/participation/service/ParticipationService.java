package com.integrityfamily.participation.service;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.ParticipationEvent;
import com.integrityfamily.domain.ParticipationEventType;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ParticipationEventRepository;
import com.integrityfamily.participation.dto.ParticipationPulseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    /**
     * Vista compacta para el widget de pulso en el dashboard.
     * Incluye 7 días de actividad diaria para la sparkline.
     */
    @Transactional(readOnly = true)
    public ParticipationPulseResponse getPulse(Long familyId) {
        LocalDateTime since = LocalDateTime.now().minusDays(ACTIVITY_WINDOW_DAYS);
        List<Long> activeMemberIds = repo.findActiveMemberIds(familyId, since);

        List<FamilyMember> allActive = familyRepository.findById(familyId)
                .map(f -> f.getMembers().stream().filter(FamilyMember::isActive).toList())
                .orElse(List.of());

        int totalMembers = allActive.size();
        int activeCount = activeMemberIds.size();

        List<ParticipationPulseResponse.MemberPulse> pulses = allActive.stream()
                .map(m -> {
                    LocalDateTime last = repo.findLastActivityByMember(familyId, m.getId()).orElse(null);
                    long days = last == null ? 999 : ChronoUnit.DAYS.between(last, LocalDateTime.now());
                    String initials = buildInitials(m.getFullName());
                    return new ParticipationPulseResponse.MemberPulse(
                            m.getId(), m.getFullName(), initials,
                            activeMemberIds.contains(m.getId()), days);
                })
                .toList();

        // Eventos de los últimos 7 días agrupados por día
        List<LocalDateTime> allEvents = repo.findAllOccurredAt(familyId, since);
        Map<LocalDate, Long> countsByDay = allEvents.stream()
                .collect(Collectors.groupingBy(dt -> dt.toLocalDate(), Collectors.counting()));

        List<ParticipationPulseResponse.DayActivity> weekly = IntStream.range(0, 7)
                .mapToObj(i -> {
                    LocalDate day = LocalDate.now().minusDays(6 - i);
                    String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es"));
                    int count = countsByDay.getOrDefault(day, 0L).intValue();
                    return new ParticipationPulseResponse.DayActivity(label, count);
                })
                .toList();

        double rate = totalMembers > 0 ? (double) activeCount / totalMembers : 0.0;
        return new ParticipationPulseResponse(totalMembers, activeCount, rate, pulses, weekly);
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase();
        String second = parts.length > 1 ? parts[1].substring(0, 1).toUpperCase() : "";
        return first + second;
    }
}
