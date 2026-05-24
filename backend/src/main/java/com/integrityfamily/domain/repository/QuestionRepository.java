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

    // Taxonomía v2 — filtros por hito y tipo
    List<Question> findByMilestoneCodeAndActiveTrue(String milestoneCode);
    List<Question> findByMilestoneCodeAndTypeAndActiveTrue(String milestoneCode, String type);
    List<Question> findByTypeAndActiveTrue(String type);
    List<Question> findByPillarNameAndActiveTrue(String pillarName);
    List<Question> findByMilestoneCodeInAndActiveTrue(java.util.Collection<String> milestoneCodes);
    List<Question> findByMilestoneCodeInAndTypeAndActiveTrue(java.util.Collection<String> milestoneCodes, String type);
}
