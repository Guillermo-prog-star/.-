package com.integrityfamily.twin.repository;

import com.integrityfamily.twin.domain.FamilyTwinProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FamilyTwinRepository extends JpaRepository<FamilyTwinProfile, Long> {
    Optional<FamilyTwinProfile> findByFamilyId(Long familyId);
}
