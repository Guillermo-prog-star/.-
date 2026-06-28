package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyCapitalSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyCapitalSnapshotRepository extends JpaRepository<FamilyCapitalSnapshot, Long> {

    List<FamilyCapitalSnapshot> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    Optional<FamilyCapitalSnapshot> findTopByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /** Snapshots de una familia desde una fecha — para trayectoria longitudinal */
    @Query("SELECT s FROM FamilyCapitalSnapshot s WHERE s.family.id = :familyId AND s.createdAt >= :since ORDER BY s.createdAt ASC")
    List<FamilyCapitalSnapshot> findByFamilyIdSince(
            @Param("familyId") Long familyId,
            @Param("since") LocalDateTime since);

    /** Snapshot más cercano a una fecha — para hitos 6m, 12m, 36m */
    @Query("SELECT s FROM FamilyCapitalSnapshot s WHERE s.family.id = :familyId AND s.createdAt <= :before ORDER BY s.createdAt DESC LIMIT 1")
    Optional<FamilyCapitalSnapshot> findLatestBeforeDate(
            @Param("familyId") Long familyId,
            @Param("before") LocalDateTime before);

    /** Para el Observatorio: todos los snapshots del mes en curso */
    @Query("SELECT s FROM FamilyCapitalSnapshot s WHERE s.createdAt >= :monthStart AND s.createdAt < :monthEnd")
    List<FamilyCapitalSnapshot> findAllInMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);
}
