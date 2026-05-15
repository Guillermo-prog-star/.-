package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.LearningEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningEntryRepository extends JpaRepository<LearningEntry, Long> {
    List<LearningEntry> findByFamilyId(Long familyId);
    List<LearningEntry> findByTaskId(Long taskId);
}
