package com.integrityfamily.adaptive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdaptiveAdjustmentRepository extends JpaRepository<AdaptiveAdjustmentEntity, UUID> {
    List<AdaptiveAdjustmentEntity> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
