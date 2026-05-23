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
}
