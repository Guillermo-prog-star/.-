package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyGratitudeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyGratitudeEntryRepository extends JpaRepository<FamilyGratitudeEntry, Long> {

    List<FamilyGratitudeEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
