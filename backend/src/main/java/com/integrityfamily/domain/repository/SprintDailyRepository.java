package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SprintDailyRepository extends JpaRepository<SprintDaily, Long> {
    List<SprintDaily> findBySprintIdOrderByCheckinDateDesc(Long sprintId);
    
    boolean existsBySprintIdAndMemberNameAndCheckinDate(Long sprintId, String memberName, LocalDate checkinDate);
}
