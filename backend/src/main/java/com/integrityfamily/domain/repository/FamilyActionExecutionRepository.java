package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyActionExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyActionExecutionRepository extends JpaRepository<FamilyActionExecution, Long> {
    Optional<FamilyActionExecution> findByFamilyIdAndActionAndIdempotencyKey(
            String familyId, String action, String idempotencyKey);
}
