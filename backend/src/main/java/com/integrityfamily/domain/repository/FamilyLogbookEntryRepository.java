package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyLogbookEntryRepository extends JpaRepository<FamilyLogbookEntry, Long> {

    List<FamilyLogbookEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<FamilyLogbookEntry> findByFamilyIdAndStatusOrderByCreatedAtDesc(
            Long familyId,
            LogbookStatus status
    );
}
