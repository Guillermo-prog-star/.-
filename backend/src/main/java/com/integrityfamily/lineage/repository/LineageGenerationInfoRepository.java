package com.integrityfamily.lineage.repository;

import com.integrityfamily.lineage.domain.LineageGenerationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineageGenerationInfoRepository extends JpaRepository<LineageGenerationInfo, Long> {

    List<LineageGenerationInfo> findByLineageIdOrderByGenerationLevel(Long lineageId);

    Optional<LineageGenerationInfo> findByLineageIdAndGenerationLevel(Long lineageId, Integer generationLevel);

    void deleteByLineageId(Long lineageId);
}
