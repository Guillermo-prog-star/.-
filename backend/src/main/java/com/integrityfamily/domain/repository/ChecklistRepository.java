package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    long countByFamilyIdAndSourceAndCompletedFalse(Long familyId, String source);
    long countByFamilyIdAndSource(Long familyId, String source);
    long countByFamilyIdAndCompletedTrue(Long familyId);
}
