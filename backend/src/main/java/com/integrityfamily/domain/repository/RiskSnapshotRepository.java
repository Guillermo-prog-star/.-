package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.RiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot, Long> {
    Optional<RiskSnapshot> findFirstByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<RiskSnapshot> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
