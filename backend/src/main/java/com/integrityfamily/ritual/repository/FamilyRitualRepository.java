package com.integrityfamily.ritual.repository;

import com.integrityfamily.ritual.domain.FamilyRitual;
import com.integrityfamily.ritual.domain.RitualStatus;
import com.integrityfamily.ritual.domain.RitualType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FamilyRitualRepository extends JpaRepository<FamilyRitual, Long> {

    List<FamilyRitual> findByFamilyIdAndStatusOrderByTriggeredAtDesc(Long familyId, RitualStatus status);

    List<FamilyRitual> findByFamilyIdOrderByTriggeredAtDesc(Long familyId);

    boolean existsByFamilyIdAndRitualTypeAndTriggeredAtAfter(
            Long familyId, RitualType type, LocalDateTime since);

    @Query("SELECT r FROM FamilyRitual r WHERE r.status IN ('PENDING','ACTIVE') ORDER BY r.triggeredAt DESC")
    List<FamilyRitual> findAllPendingOrActive();
}
