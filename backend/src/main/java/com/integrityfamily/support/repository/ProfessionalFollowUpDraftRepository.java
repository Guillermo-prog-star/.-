package com.integrityfamily.support.repository;

import com.integrityfamily.support.domain.DraftStatus;
import com.integrityfamily.support.domain.ProfessionalFollowUpDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessionalFollowUpDraftRepository extends JpaRepository<ProfessionalFollowUpDraft, Long> {
    List<ProfessionalFollowUpDraft> findByAssignmentIdAndStatus(Long assignmentId, DraftStatus status);
    List<ProfessionalFollowUpDraft> findByAssignmentIdOrderByGeneratedAtDesc(Long assignmentId);
}
