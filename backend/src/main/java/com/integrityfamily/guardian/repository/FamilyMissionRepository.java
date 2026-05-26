package com.integrityfamily.guardian.repository;

import com.integrityfamily.guardian.domain.FamilyMission;
import com.integrityfamily.guardian.domain.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FamilyMissionRepository extends JpaRepository<FamilyMission, Long> {

    List<FamilyMission> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    Optional<FamilyMission> findTopByFamilyIdAndStatusOrderByActivatedAtDesc(Long familyId, MissionStatus status);

    long countByFamilyIdAndStatus(Long familyId, MissionStatus status);
}
