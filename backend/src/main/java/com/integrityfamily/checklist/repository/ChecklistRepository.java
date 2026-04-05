package com.integrityfamily.checklist.repository;

import com.integrityfamily.checklist.domain.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChecklistRepository extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findByFamilyId(Long familyId);
}