package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyMetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMetricsSnapshotRepository extends JpaRepository<FamilyMetricsSnapshot, Long> {
    List<FamilyMetricsSnapshot> findByFamilyIdOrderBySnapshotDateAsc(Long familyId);
    List<FamilyMetricsSnapshot> findByFamilyIdOrderBySnapshotDateDesc(Long familyId);
}
