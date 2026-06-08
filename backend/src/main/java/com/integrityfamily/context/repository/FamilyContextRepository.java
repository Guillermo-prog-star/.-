package com.integrityfamily.context.repository;

import com.integrityfamily.context.domain.FamilyContextSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FamilyContextRepository extends JpaRepository<FamilyContextSnapshot, Long> {
    Optional<FamilyContextSnapshot> findByFamilyId(Long familyId);
}
