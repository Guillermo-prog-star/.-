package com.integrityfamily.common.repository;

import com.integrityfamily.common.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByFamilyIdOrderBySentAtDesc(Long familyId);
    
    long countByFamilyIdAndType(Long familyId, String type);
}


