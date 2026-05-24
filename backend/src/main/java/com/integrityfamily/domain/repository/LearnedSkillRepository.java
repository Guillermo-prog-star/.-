package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.LearnedSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearnedSkillRepository extends JpaRepository<LearnedSkill, Long> {

    Optional<LearnedSkill> findBySkillName(String skillName);

    List<LearnedSkill> findByDimensionOrderBySuccessRateDesc(String dimension);

    /** Skills con suficiente confianza para aplicarse automáticamente */
    @Query("SELECT s FROM LearnedSkill s WHERE s.confidence >= :minConfidence ORDER BY s.successRate DESC")
    List<LearnedSkill> findHighConfidenceSkills(@Param("minConfidence") Double minConfidence);

    /** Skills más reutilizadas — las más validadas empíricamente */
    @Query("SELECT s FROM LearnedSkill s ORDER BY s.reuseCount DESC")
    List<LearnedSkill> findMostUsedSkills();

    boolean existsBySkillName(String skillName);
}
