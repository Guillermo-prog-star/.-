package com.integrityfamily.lts.repository;

import com.integrityfamily.lts.domain.LearningError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository extends JpaRepository<LearningError, Long> {
}
