package com.integrityfamily.lineage.repository;

import com.integrityfamily.lineage.domain.LineageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineageEventRepository extends JpaRepository<LineageEvent, Long> {

    List<LineageEvent> findByMemberIdOrderBySortOrderAsc(Long memberId);
}
