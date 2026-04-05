package com.integrityfamily.assessment.repository;

import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.dto.QuestionStatDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * QuestionRepository: Acceso a datos para el Banco de Preguntas.
 * Centraliza la lógica de aleatoriedad y estadísticas para el Nodo Armenia.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Verifica duplicados por llave única (REC_EMO_001, etc.)
     */
    boolean existsByQuestionKey(String questionKey);

    /**
     * Obtiene reactivos aleatorios para evitar monotonía en el diagnóstico.
     */
    @Query(value = "SELECT * FROM questions WHERE dimension = :dimension AND active = true ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByDimension(@Param("dimension") String dimension, @Param("limit") int limit);

    /**
     * Consulta de Control de Calidad: Agrupa y cuenta preguntas por jerarquía.
     * Utiliza proyección sobre el DTO QuestionStatDTO.
     */
    @Query("SELECT new com.integrityfamily.assessment.dto.QuestionStatDTO(q.dimension, q.area, COUNT(q)) " +
           "FROM Question q GROUP BY q.dimension, q.area ORDER BY q.dimension, q.area")
    List<QuestionStatDTO> getUsageStatistics();
}