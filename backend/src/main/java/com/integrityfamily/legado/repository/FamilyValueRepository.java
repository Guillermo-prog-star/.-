package com.integrityfamily.legado.repository;

import com.integrityfamily.legado.domain.FamilyValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyValueRepository extends JpaRepository<FamilyValue, Long> {
    List<FamilyValue> findByFamilyIdOrderBySortOrder(Long familyId);
    void deleteByFamilyId(Long familyId);
}
