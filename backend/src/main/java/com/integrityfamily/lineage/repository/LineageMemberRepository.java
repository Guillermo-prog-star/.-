package com.integrityfamily.lineage.repository;

import com.integrityfamily.lineage.domain.LineageMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LineageMemberRepository extends JpaRepository<LineageMember, Long> {

    List<LineageMember> findByLineageIdOrderByGenerationAsc(Long lineageId);

    @Query("SELECT m FROM LineageMember m LEFT JOIN FETCH m.events WHERE m.lineage.id = :lineageId ORDER BY m.generation ASC")
    List<LineageMember> findWithEventsByLineageId(@Param("lineageId") Long lineageId);
}
