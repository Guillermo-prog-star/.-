package com.integrityfamily.vitality.repository;

import com.integrityfamily.vitality.domain.DailyVitalityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyVitalityLogRepository extends JpaRepository<DailyVitalityLog, Long> {

    Optional<DailyVitalityLog> findByFamilyMemberIdAndLogDate(Long familyMemberId, LocalDate logDate);

    List<DailyVitalityLog> findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(
            Long familyMemberId, LocalDate from, LocalDate to);
}
