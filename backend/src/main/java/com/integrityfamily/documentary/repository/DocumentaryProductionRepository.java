package com.integrityfamily.documentary.repository;

import com.integrityfamily.documentary.domain.DocumentaryProduction;
import com.integrityfamily.documentary.domain.DocumentaryScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentaryProductionRepository extends JpaRepository<DocumentaryProduction, Long> {
    List<DocumentaryProduction> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<DocumentaryProduction> findByFamilyIdAndScopeOrderByCreatedAtDesc(Long familyId, DocumentaryScope scope);
}
