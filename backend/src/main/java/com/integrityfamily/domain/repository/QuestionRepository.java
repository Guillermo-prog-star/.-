package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * SDD SPEC: Repositorio centralizado de reactivos psicopedagógicos.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByActiveTrue();
    List<Question> findByDimension(String dimension);
    java.util.Optional<Question> findByQuestionKey(String questionKey);
}
