package com.integrityfamily.lts.repository;

import com.integrityfamily.lts.domain.Hypothesis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HypothesisRepository extends JpaRepository<Hypothesis, Long> {
}
