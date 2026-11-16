package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SafetyProtocolActivation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyProtocolActivationRepository extends JpaRepository<SafetyProtocolActivation, Long> {
    List<SafetyProtocolActivation> findByFamilyTrajectoryIdOrderByCreatedAtDesc(Long familyTrajectoryId);
    List<SafetyProtocolActivation> findByFamilyTrajectoryIdAndClosedFalse(Long familyTrajectoryId);
}
