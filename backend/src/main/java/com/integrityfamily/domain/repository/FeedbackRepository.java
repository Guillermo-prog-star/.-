package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    List<Feedback> findByScoreLessThanEqualAndCreatedAtAfter(int score, LocalDateTime time);
}
