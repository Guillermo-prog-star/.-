package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    java.util.List<AuditEvent> findByActorEmailInOrderByOccurredAtDesc(java.util.List<String> emails);
}


