package com.integrityfamily.legado.repository;

import com.integrityfamily.legado.domain.FamilyLegacy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FamilyLegacyRepository extends JpaRepository<FamilyLegacy, Long> {
    Optional<FamilyLegacy> findByFamilyId(Long familyId);
}
