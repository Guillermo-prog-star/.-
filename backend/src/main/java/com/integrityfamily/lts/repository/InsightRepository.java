package com.integrityfamily.lts.repository;

import com.integrityfamily.lts.domain.Insight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InsightRepository extends JpaRepository<Insight, Long> {
    List<Insight> findBySessionId(Long sessionId);
}
