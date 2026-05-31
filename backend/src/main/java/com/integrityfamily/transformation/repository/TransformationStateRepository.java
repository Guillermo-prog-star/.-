package com.integrityfamily.transformation.repository;

import com.integrityfamily.transformation.domain.TransformationState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransformationStateRepository extends JpaRepository<TransformationState, Long> {
    Optional<TransformationState> findByFamilyId(Long familyId);
}
