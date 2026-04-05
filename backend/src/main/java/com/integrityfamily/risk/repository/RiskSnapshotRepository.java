package com.integrityfamily.risk.repository;
import com.integrityfamily.risk.domain.RiskSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface RiskSnapshotRepository extends JpaRepository<RiskSnapshot,Long> {
    List<RiskSnapshot> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    Optional<RiskSnapshot> findTopByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
