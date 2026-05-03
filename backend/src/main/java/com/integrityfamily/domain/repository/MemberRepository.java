package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByFamilyId(Long familyId);
    Optional<FamilyMember> findByEmail(String email);
    boolean existsByEmail(String email);
}
