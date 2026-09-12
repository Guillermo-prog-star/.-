package com.integrityfamily.support.repository;

import com.integrityfamily.support.domain.SupportAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportAccessLogRepository extends JpaRepository<SupportAccessLog, Long> {

    List<SupportAccessLog> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
}
