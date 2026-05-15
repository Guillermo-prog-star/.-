package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.AiInferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiInferenceRepository extends JpaRepository<AiInferenceEntity, Long> {
    List<AiInferenceEntity> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    AiInferenceEntity findFirstByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
