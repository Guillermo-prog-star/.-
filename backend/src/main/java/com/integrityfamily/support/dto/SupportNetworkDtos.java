package com.integrityfamily.support.dto;

import com.integrityfamily.support.domain.AssignmentStatus;
import com.integrityfamily.support.domain.SupportSpecialty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class SupportNetworkDtos {

    // ── Registro de profesional ───────────────────────────────────────────

    @Data
    public static class RegisterProfessionalRequest {
        private String fullName;
        private String email;
        private String phone;
        private SupportSpecialty specialty;
        private String licenseNumber;
        private String institutionName;
        private String bio;
    }

    // ── Invitación de la familia a un profesional ─────────────────────────

    @Data
    public static class InviteRequest {
        private Long supportMemberId;
        private AccessScopeDto accessScope;
        private String initialNote;
    }

    @Data
    public static class AccessScopeDto {
        private boolean canViewIcfScore      = true;
        private boolean canViewRiskLevel     = true;
        private boolean canViewPlanSummary   = false;
        private boolean canViewSprintProgress = false;
        private boolean canViewCrisisHistory  = false;
        private boolean canLeaveNotes         = true;
    }

    // ── Consentimiento ────────────────────────────────────────────────────

    @Data
    public static class ConsentRequest {
        private Long assignmentId;
        private AccessScopeDto accessScope; // la familia puede ajustar el alcance al consentir
    }

    // ── Revocación ────────────────────────────────────────────────────────

    @Data
    public static class RevokeRequest {
        private Long assignmentId;
        private String reason;
    }

    // ── Nota profesional ──────────────────────────────────────────────────

    @Data
    public static class AddNoteRequest {
        private Long assignmentId;
        private String content;
        private boolean visibleToFamily;
    }

    // ── Respuestas ────────────────────────────────────────────────────────

    @Data @Builder
    public static class ProfessionalResponse {
        private Long id;
        private String fullName;
        private String email;
        private SupportSpecialty specialty;
        private String licenseNumber;
        private String institutionName;
        private String bio;
    }

    @Data @Builder
    public static class AssignmentResponse {
        private Long id;
        private Long familyId;
        private ProfessionalResponse professional;
        private SupportSpecialty specialty;
        private AssignmentStatus status;
        private String invitedByEmail;
        private LocalDateTime invitedAt;
        private String consentedByEmail;
        private LocalDateTime consentedAt;
        private AccessScopeDto accessScope;
        private List<NoteResponse> visibleNotes;
    }

    @Data @Builder
    public static class NoteResponse {
        private Long id;
        private String content;
        private boolean visibleToFamily;
        private LocalDateTime createdAt;
    }

    @Data @Builder
    public static class FamilySupportSummary {
        private Long familyId;
        private int totalProfessionals;
        private int activeProfessionals;
        private List<AssignmentResponse> assignments;
    }
}
