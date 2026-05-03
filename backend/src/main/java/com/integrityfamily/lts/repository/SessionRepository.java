package com.integrityfamily.lts.repository;

import com.integrityfamily.lts.domain.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<LearningSession, Long> {
    List<LearningSession> findByFamilyId(Long familyId);
    List<LearningSession> findByMemberId(Long memberId);
}
