package com.integrityfamily.evaluation.repository;

import com.integrityfamily.evaluation.domain.EvaluationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EvaluationAnswerRepository extends JpaRepository<EvaluationAnswer, Long> {
    
    @Modifying
    @Transactional
    @Query("DELETE FROM EvaluationAnswer a WHERE a.evaluation.id = :evaluationId")
    void deleteByEvaluationId(Long evaluationId);
}
