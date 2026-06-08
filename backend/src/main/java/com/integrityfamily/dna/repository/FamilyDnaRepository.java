package com.integrityfamily.dna.repository;

import com.integrityfamily.dna.domain.FamilyDna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FamilyDnaRepository extends JpaRepository<FamilyDna, Long> {
    Optional<FamilyDna> findByFamilyId(Long familyId);
    boolean existsByFamilyId(Long familyId);
}
