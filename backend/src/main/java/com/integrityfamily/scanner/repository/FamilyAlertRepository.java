package com.integrityfamily.scanner.repository;

import com.integrityfamily.scanner.domain.FamilyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyAlertRepository extends JpaRepository<FamilyAlert, Long> {

    List<FamilyAlert> findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(Long familyId);

    List<FamilyAlert> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    long countByFamilyIdAndResolvedFalse(Long familyId);

    boolean existsByFamilyIdAndAlertTypeAndResolvedFalse(Long familyId, String alertType);
}
