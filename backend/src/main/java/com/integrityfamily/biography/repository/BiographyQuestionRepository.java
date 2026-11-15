package com.integrityfamily.biography.repository;

import com.integrityfamily.biography.domain.BiographyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiographyQuestionRepository extends JpaRepository<BiographyQuestion, Long> {
    List<BiographyQuestion> findAllByOrderByChapterNumberAscSortOrderAsc();
    Optional<BiographyQuestion> findByQuestionKey(String questionKey);
}
