package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.JournalEntry;
import com.integrityfamily.domain.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<JournalEntry> findByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, JournalStatus status);
}
