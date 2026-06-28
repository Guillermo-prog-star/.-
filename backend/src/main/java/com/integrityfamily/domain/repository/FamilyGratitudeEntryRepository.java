package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyGratitudeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FamilyGratitudeEntryRepository extends JpaRepository<FamilyGratitudeEntry, Long> {

    List<FamilyGratitudeEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /** IND-03: entradas de gratitud en ventana de 30 días */
    @Query("SELECT COUNT(g) FROM FamilyGratitudeEntry g " +
           "WHERE g.family.id = :familyId AND g.createdAt >= :since")
    long countByFamilyIdSince(@Param("familyId") Long familyId,
                               @Param("since") LocalDateTime since);
}
