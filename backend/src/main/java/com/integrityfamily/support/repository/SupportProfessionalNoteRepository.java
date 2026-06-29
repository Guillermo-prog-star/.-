package com.integrityfamily.support.repository;

import com.integrityfamily.support.domain.SupportProfessionalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportProfessionalNoteRepository extends JpaRepository<SupportProfessionalNote, Long> {
    List<SupportProfessionalNote> findByAssignmentIdOrderByCreatedAtDesc(Long assignmentId);
    List<SupportProfessionalNote> findByFamilyIdAndVisibleToFamilyTrueOrderByCreatedAtDesc(Long familyId);
}
