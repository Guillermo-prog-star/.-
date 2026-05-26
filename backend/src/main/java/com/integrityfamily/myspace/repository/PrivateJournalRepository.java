package com.integrityfamily.myspace.repository;

import com.integrityfamily.myspace.domain.PrivateJournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivateJournalRepository extends JpaRepository<PrivateJournalEntry, Long> {
    List<PrivateJournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);
}
