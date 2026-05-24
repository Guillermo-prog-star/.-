package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.NarrativeChapter;
import com.integrityfamily.domain.NarrativeChapter.NarrativePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NarrativeChapterRepository extends JpaRepository<NarrativeChapter, Long> {

    List<NarrativeChapter> findByFamilyIdOrderByChapterNumberAsc(Long familyId);

    /** Capítulo actualmente abierto (closedAt IS NULL) */
    @Query("SELECT c FROM NarrativeChapter c WHERE c.family.id = :familyId AND c.closedAt IS NULL")
    Optional<NarrativeChapter> findOpenChapterByFamilyId(@Param("familyId") Long familyId);

    /** Número máximo de capítulo para esta familia */
    @Query("SELECT COALESCE(MAX(c.chapterNumber), 0) FROM NarrativeChapter c WHERE c.family.id = :familyId")
    int countChaptersByFamilyId(@Param("familyId") Long familyId);

    /** Capítulos que marcaron puntos de inflexión */
    List<NarrativeChapter> findByFamilyIdAndTurningPointTrueOrderByChapterNumberAsc(Long familyId);

    /** Último capítulo cerrado */
    @Query("SELECT c FROM NarrativeChapter c WHERE c.family.id = :familyId AND c.closedAt IS NOT NULL ORDER BY c.chapterNumber DESC")
    List<NarrativeChapter> findClosedChaptersByFamilyIdDesc(@Param("familyId") Long familyId);

    boolean existsByFamilyIdAndPhase(Long familyId, NarrativePhase phase);
}
