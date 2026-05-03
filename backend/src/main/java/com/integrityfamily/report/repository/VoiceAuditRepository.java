package com.integrityfamily.report.repository;

import com.integrityfamily.report.domain.VoiceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VoiceAuditRepository extends JpaRepository<VoiceAudit, Long> {

    List<VoiceAudit> findByFamilyId(Long familyId);

    @Query("SELECT COUNT(v) FROM VoiceAudit v WHERE v.success = true")
    long countSuccessfulMessages();

    @Query("SELECT COUNT(DISTINCT v.familyId) FROM VoiceAudit v")
    long countDistinctFamilyId();

    @Query("SELECT COALESCE(SUM(v.durationSeconds), 0) FROM VoiceAudit v")
    long sumDurationSeconds();

    @Query("SELECT v.municipio, COUNT(v) FROM VoiceAudit v GROUP BY v.municipio")
    List<Object[]> getRegionalUsage();

    List<VoiceAudit> findTop10ByOrderByProcessedAtDesc();
}


