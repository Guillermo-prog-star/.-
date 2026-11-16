package com.integrityfamily.sistemas.repository;

import com.integrityfamily.sistemas.domain.IfSystemCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IfSystemCheckRepository extends JpaRepository<IfSystemCheck, Long> {

    List<IfSystemCheck> findTop12ByFamily_IdOrderByCreatedAtDesc(Long familyId);
}
