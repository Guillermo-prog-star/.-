package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * SDD: Repositorio central para la Bitácora de Transformación.
 */
@Repository
public interface FamilyLogbookRepository extends JpaRepository<FamilyLogbookEntry, Long> {
    List<FamilyLogbookEntry> findByFamilyId(Long familyId);
    
    // Métodos requeridos por AnalyticsServiceImpl
    List<FamilyLogbookEntry> findByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, LogbookStatus status);
    List<FamilyLogbookEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
