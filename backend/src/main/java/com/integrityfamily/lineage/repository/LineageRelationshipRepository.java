package com.integrityfamily.lineage.repository;

import com.integrityfamily.lineage.domain.LineageRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineageRelationshipRepository extends JpaRepository<LineageRelationship, Long> {
    List<LineageRelationship> findByLineageId(Long lineageId);
    void deleteByLineageId(Long lineageId);
}
