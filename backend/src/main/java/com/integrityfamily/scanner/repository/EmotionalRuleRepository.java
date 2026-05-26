package com.integrityfamily.scanner.repository;

import com.integrityfamily.scanner.domain.EmotionalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmotionalRuleRepository extends JpaRepository<EmotionalRule, Long> {

    /** JOIN FETCH para evitar N+1 al cargar requiredSignals (@ElementCollection). */
    @Query("SELECT DISTINCT r FROM EmotionalRule r LEFT JOIN FETCH r.requiredSignals ORDER BY r.id ASC")
    List<EmotionalRule> findAllWithSignals();

    @Query("SELECT DISTINCT r FROM EmotionalRule r LEFT JOIN FETCH r.requiredSignals WHERE r.active = true")
    List<EmotionalRule> findByActiveTrue();

    List<EmotionalRule> findByMemberRoleAndActiveTrue(String memberRole);

    List<EmotionalRule> findByRuleKeyOrderByVersionDesc(String ruleKey);
}
