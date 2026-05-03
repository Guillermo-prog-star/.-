package com.integrityfamily.lts.repository;

import com.integrityfamily.lts.domain.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findBySessionIdOrderByVersionAsc(Long sessionId);
}
