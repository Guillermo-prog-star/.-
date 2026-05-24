package com.integrityfamily.analytics.repository;

import com.integrityfamily.analytics.domain.ProgressSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressSnapshotRepository extends JpaRepository<ProgressSnapshot, Long> {
    List<ProgressSnapshot> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    Optional<ProgressSnapshot> findFirstByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
