package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyDocumentary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FamilyDocumentaryRepository extends JpaRepository<FamilyDocumentary, Long> {
    List<FamilyDocumentary> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
