package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyLongitudinalState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyLongitudinalStateRepository extends JpaRepository<FamilyLongitudinalState, Long> {

    Optional<FamilyLongitudinalState> findByFamilyId(Long familyId);

    @Query("SELECT s FROM FamilyLongitudinalState s WHERE s.family.id = :familyId")
    Optional<FamilyLongitudinalState> findByFamilyIdJoined(@Param("familyId") Long familyId);

    /** Familias en deterioro activo — para reportes sentinel */
    @Query("SELECT s FROM FamilyLongitudinalState s WHERE s.consecutiveDeteriorations >= 3")
    java.util.List<FamilyLongitudinalState> findFamiliesWithSustainedDeterioration();

    /** Familias con crisis reciente — para priorización de IA */
    @Query("SELECT s FROM FamilyLongitudinalState s WHERE s.lastCrisisAt >= :since")
    java.util.List<FamilyLongitudinalState> findFamiliesWithRecentCrisis(
            @Param("since") java.time.LocalDateTime since);
}
