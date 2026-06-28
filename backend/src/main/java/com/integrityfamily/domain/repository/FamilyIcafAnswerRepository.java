package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyIcafAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyIcafAnswerRepository extends JpaRepository<FamilyIcafAnswer, Long> {

    List<FamilyIcafAnswer> findByFamilyId(Long familyId);

    Optional<FamilyIcafAnswer> findByFamilyIdAndQuestionKey(Long familyId, String questionKey);

    List<FamilyIcafAnswer> findByFamilyIdAndIcafDomain(Long familyId, String icafDomain);

    /** Score promedio de un dominio para una familia (0.0 si no hay respuestas) */
    @Query("SELECT COALESCE(AVG(a.score), 0.0) FROM FamilyIcafAnswer a " +
           "WHERE a.family.id = :familyId AND a.icafDomain = :domain")
    Double avgScoreByDomain(@Param("familyId") Long familyId, @Param("domain") String domain);

    /** Cuántas preguntas del dominio ya fueron respondidas */
    @Query("SELECT COUNT(a) FROM FamilyIcafAnswer a " +
           "WHERE a.family.id = :familyId AND a.icafDomain = :domain")
    long countAnsweredByDomain(@Param("familyId") Long familyId, @Param("domain") String domain);

    /** true si la familia ya completó al menos una respuesta de un dominio */
    default boolean hasAnswers(Long familyId, String domain) {
        return countAnsweredByDomain(familyId, domain) > 0;
    }

    /** IND-07: respondedores distintos del cuestionario ICaF en los últimos N días */
    @Query("SELECT COUNT(DISTINCT a.answeredBy) FROM FamilyIcafAnswer a " +
           "WHERE a.family.id = :familyId AND a.answeredAt >= :since")
    long countDistinctRespondersSince(@Param("familyId") Long familyId,
                                       @Param("since") java.time.LocalDateTime since);
}
