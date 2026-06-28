package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilySprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilySprintRepository extends JpaRepository<FamilySprint, Long> {

    List<FamilySprint> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    @Query("SELECT s FROM FamilySprint s WHERE s.family.id = :familyId AND s.status = 'ACTIVE'")
    Optional<FamilySprint> findActiveSprintForFamily(Long familyId);

    /** IND-02: sprints activos con su duración y fechas para calcular dailies esperados */
    @Query("SELECT s FROM FamilySprint s WHERE s.family.id = :familyId AND s.status IN ('ACTIVE','COMPLETED')")
    List<FamilySprint> findActiveAndCompletedByFamilyId(
            @org.springframework.data.repository.query.Param("familyId") Long familyId);

    /** IND-10: total de sprints de la familia */
    long countByFamilyId(Long familyId);
}
