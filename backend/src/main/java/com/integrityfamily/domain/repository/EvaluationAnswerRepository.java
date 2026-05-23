package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.EvaluationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationAnswerRepository extends JpaRepository<EvaluationAnswer, Long> {

    /** Todas las respuestas de una evaluación, en orden de contestación. */
    List<EvaluationAnswer> findByEvaluationIdOrderByAnsweredAtAsc(Long evaluationId);

    /** Busca la respuesta de una pregunta concreta para detectar si ya fue contestada (upsert). */
    Optional<EvaluationAnswer> findByEvaluationIdAndQuestionKey(Long evaluationId, String questionKey);

    /** Cuántas preguntas ha respondido el usuario hasta ahora. */
    long countByEvaluationId(Long evaluationId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EvaluationAnswer a WHERE a.evaluation.id = :evaluationId")
    void deleteByEvaluationId(Long evaluationId);
}
