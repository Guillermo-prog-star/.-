package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.RiskTrajectory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskTrajectoryRepository extends JpaRepository<RiskTrajectory, Long> {
    List<RiskTrajectory> findByActiveTrue();
    List<RiskTrajectory> findByMacrodomainAndActiveTrue(RiskMacrodomain macrodomain);
    Optional<RiskTrajectory> findByCode(String code);
}
