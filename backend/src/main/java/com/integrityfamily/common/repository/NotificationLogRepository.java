package com.integrityfamily.common.repository;

import com.integrityfamily.common.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByFamilyIdOrderBySentAtDesc(Long familyId);

    List<NotificationLog> findByFamilyIdOrderBySentAtDesc(Long familyId, Pageable pageable);

    long countByFamilyIdAndType(Long familyId, String type);

    long countByFamilyIdAndViewedFalse(Long familyId);

    @Modifying
    @Query("UPDATE NotificationLog n SET n.viewed = true WHERE n.family.id = :familyId AND n.viewed = false")
    void markAllViewedByFamilyId(Long familyId);
}


