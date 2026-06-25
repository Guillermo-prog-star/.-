package com.integrityfamily.documentation.repository;

import com.integrityfamily.documentation.domain.DocumentCategory;
import com.integrityfamily.documentation.domain.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    List<ProjectDocument> findByCategoryAndStatusOrderByTitleAsc(DocumentCategory category, String status);

    List<ProjectDocument> findByStatusOrderByCategoryAscTitleAsc(String status);

    Optional<ProjectDocument> findByCode(String code);

    @Query("""
        SELECT d FROM ProjectDocument d
        WHERE d.status = 'ACTIVE'
        AND (
            LOWER(d.title)   LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(d.summary) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(d.tags)    LIKE LOWER(CONCAT('%', :q, '%'))
        )
        ORDER BY d.category, d.title
    """)
    List<ProjectDocument> searchByKeyword(@Param("q") String query);

    @Query("SELECT d FROM ProjectDocument d WHERE d.status = 'ACTIVE' ORDER BY d.category, d.title")
    List<ProjectDocument> findAllActive();
}
