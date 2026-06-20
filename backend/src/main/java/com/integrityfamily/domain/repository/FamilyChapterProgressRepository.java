package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyChapterProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FamilyChapterProgressRepository extends JpaRepository<FamilyChapterProgress, Long> {

    List<FamilyChapterProgress> findByFamilyIdOrderByChapterNumber(Long familyId);

    Optional<FamilyChapterProgress> findByFamilyIdAndChapterNumber(Long familyId, Integer chapterNumber);

    @Query("SELECT COUNT(p) FROM FamilyChapterProgress p WHERE p.familyId = :familyId AND p.completed = true")
    int countCompletedByFamilyId(Long familyId);

    @Query("SELECT MAX(p.chapterNumber) FROM FamilyChapterProgress p WHERE p.familyId = :familyId")
    Optional<Integer> findMaxChapterByFamilyId(Long familyId);
}
