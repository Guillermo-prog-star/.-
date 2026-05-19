package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyIdentityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyIdentityProfileRepository extends JpaRepository<FamilyIdentityProfile, Long> {

    Optional<FamilyIdentityProfile> findByFamilyId(Long familyId);
}
