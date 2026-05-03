package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.CriticalDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CriticalDayRepository extends JpaRepository<CriticalDay, Long> {
    List<CriticalDay> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
