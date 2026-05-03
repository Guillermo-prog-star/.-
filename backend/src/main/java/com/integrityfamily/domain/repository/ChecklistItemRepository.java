package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByFamilyId(Long familyId);
    List<ChecklistItem> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
