package com.integrityfamily.sistemas.repository;

import com.integrityfamily.sistemas.domain.IfSystemReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IfSystemReviewRepository extends JpaRepository<IfSystemReview, Long> {

    @EntityGraph(attributePaths = {"items", "author"})
    List<IfSystemReview> findByFamily_IdOrderByReviewDateDescCreatedAtDesc(Long familyId);

    @Query("""
            SELECT r FROM IfSystemReview r
            WHERE r.family.id = :familyId
              AND r.reviewDate BETWEEN :start AND :end
            """)
    List<IfSystemReview> findInWindow(@Param("familyId") Long familyId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}
