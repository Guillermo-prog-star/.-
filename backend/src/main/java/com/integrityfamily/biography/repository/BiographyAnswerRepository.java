package com.integrityfamily.biography.repository;

import com.integrityfamily.biography.domain.BiographyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiographyAnswerRepository extends JpaRepository<BiographyAnswer, Long> {

    Optional<BiographyAnswer> findByMemberIdAndQuestionId(Long memberId, Long questionId);

    /*
     * Regla de visibilidad (ADR-017): visible solo si el viewer es el propio autor o si
     * la respuesta está SHARED_WITH_FAMILY. Deliberadamente SIN bypass para admin/creador
     * de familia (a diferencia de JournalEntryRepository, ADR-012). viewerMemberId null
     * (viewer sin perfil de miembro) → `a.member.id = NULL` es falso en SQL → solo ve lo
     * compartido.
     */

    @Query("SELECT a FROM BiographyAnswer a WHERE a.family.id = :familyId AND a.member.id = :targetMemberId " +
           "AND (a.member.id = :viewerMemberId " +
           "     OR a.visibility = com.integrityfamily.domain.EntryVisibility.SHARED_WITH_FAMILY) " +
           "ORDER BY a.question.chapterNumber ASC, a.question.sortOrder ASC")
    List<BiographyAnswer> findVisibleToMember(@Param("familyId") Long familyId,
                                               @Param("targetMemberId") Long targetMemberId,
                                               @Param("viewerMemberId") Long viewerMemberId);

    /** "Otra perspectiva": respuestas de la familia etiquetadas con la misma escena, con la misma regla de visibilidad. */
    @Query("SELECT a FROM BiographyAnswer a WHERE a.family.id = :familyId AND a.perceivedEventKey = :perceivedEventKey " +
           "AND (a.member.id = :viewerMemberId " +
           "     OR a.visibility = com.integrityfamily.domain.EntryVisibility.SHARED_WITH_FAMILY)")
    List<BiographyAnswer> findPerspectivesVisibleToMember(@Param("familyId") Long familyId,
                                                           @Param("perceivedEventKey") String perceivedEventKey,
                                                           @Param("viewerMemberId") Long viewerMemberId);
}
