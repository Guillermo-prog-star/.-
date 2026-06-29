package com.integrityfamily.family.service;

import com.integrityfamily.domain.FamilyJourneySnapshot;
import com.integrityfamily.domain.repository.FamilyJourneySnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.dto.JourneyHistoryResponse;
import com.integrityfamily.family.dto.JourneyHistoryResponse.SnapshotPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JourneyHistoryService {

    private final FamilyJourneySnapshotRepository snapshotRepository;
    private final FamilyRepository                familyRepository;

    @Transactional(readOnly = true)
    public JourneyHistoryResponse getHistory(Long familyId) {
        var family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        List<FamilyJourneySnapshot> snapshots =
                snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(familyId);

        List<SnapshotPoint> points = snapshots.stream()
                .map(s -> new SnapshotPoint(
                        s.getSnapshotDate(),
                        s.getJourneyLevel(),
                        s.getJourneyProgress(),
                        s.isLevelUp(),
                        s.getPreviousLevel()
                ))
                .toList();

        long levelUps = snapshots.stream().filter(FamilyJourneySnapshot::isLevelUp).count();

        return new JourneyHistoryResponse(
                familyId,
                family.getName(),
                points,
                (int) levelUps,
                points.isEmpty() ? null : points.get(0).date(),
                points.isEmpty() ? null : points.get(points.size() - 1).date()
        );
    }
}
