package com.integrityfamily.support.repository;

import com.integrityfamily.support.domain.AssignmentStatus;
import com.integrityfamily.support.domain.FamilySupportAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilySupportAssignmentRepository extends JpaRepository<FamilySupportAssignment, Long> {
    List<FamilySupportAssignment> findByFamilyId(Long familyId);
    List<FamilySupportAssignment> findByFamilyIdAndStatus(Long familyId, AssignmentStatus status);
    Optional<FamilySupportAssignment> findByFamilyIdAndSupportMemberId(Long familyId, Long supportMemberId);
    boolean existsByFamilyIdAndSupportMemberIdAndStatusNot(Long familyId, Long supportMemberId, AssignmentStatus status);
}
