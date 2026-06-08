package com.integrityfamily.twin.repository;

import com.integrityfamily.twin.domain.FamilyPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FamilyPredictionRepository extends JpaRepository<FamilyPrediction, Long> {
    List<FamilyPrediction> findByFamilyIdAndStatusOrderByConfidenceDesc(Long familyId, String status);
    List<FamilyPrediction> findByFamilyIdOrderByPredictedAtDesc(Long familyId);
    void deleteByFamilyIdAndStatus(Long familyId, String status);
}
