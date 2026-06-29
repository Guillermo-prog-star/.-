package com.integrityfamily.ecosystem.repository;

import com.integrityfamily.ecosystem.domain.EcosystemAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcosystemAccessLogRepository extends JpaRepository<EcosystemAccessLog, Long> {
    List<EcosystemAccessLog> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<EcosystemAccessLog> findByLinkIdOrderByCreatedAtDesc(Long linkId);
}
