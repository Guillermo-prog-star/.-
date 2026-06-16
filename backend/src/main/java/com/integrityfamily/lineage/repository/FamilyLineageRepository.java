package com.integrityfamily.lineage.repository;

import com.integrityfamily.lineage.domain.FamilyLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FamilyLineageRepository extends JpaRepository<FamilyLineage, Long> {

    Optional<FamilyLineage> findByFamilyId(Long familyId);

    boolean existsByFamilyId(Long familyId);

    boolean existsByLineageCode(String lineageCode);

    @Query("SELECT fl FROM FamilyLineage fl LEFT JOIN FETCH fl.members WHERE fl.family.id = :familyId")
    Optional<FamilyLineage> findWithMembersByFamilyId(@Param("familyId") Long familyId);
}
