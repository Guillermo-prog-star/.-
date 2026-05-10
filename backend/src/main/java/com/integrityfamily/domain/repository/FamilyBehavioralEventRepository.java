package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyBehavioralEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyBehavioralEventRepository extends JpaRepository<FamilyBehavioralEvent, Long> {

    List<FamilyBehavioralEvent> findByFamilyIdOrderByOccurredAtDesc(Long familyId);
}
