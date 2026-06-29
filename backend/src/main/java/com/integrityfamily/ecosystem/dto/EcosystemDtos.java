package com.integrityfamily.ecosystem.dto;

import com.integrityfamily.ecosystem.domain.EcosystemLinkStatus;
import com.integrityfamily.ecosystem.domain.NetworkType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EcosystemDtos {

    // ── Registro de participante ──────────────────────────────────────────

    @Data
    public static class RegisterParticipantRequest {
        private String name;
        private NetworkType networkType;
        private String description;
        private String contactEmail;
        private String contactPhone;
        private String website;
    }

    // ── Vincular familia a participante ───────────────────────────────────

    @Data
    public static class LinkRequest {
        private Long participantId;
        private String objective;
        private String responsibilities;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private EcosystemAccessScopeDto accessScope;
    }

    // ── Consentimiento ────────────────────────────────────────────────────

    @Data
    public static class ConsentRequest {
        private Long linkId;
        private EcosystemAccessScopeDto accessScope;
    }

    // ── Revocación ────────────────────────────────────────────────────────

    @Data
    public static class RevokeRequest {
        private Long linkId;
        private String reason;
    }

    // ── Alcance de acceso (mínimo privilegio por defecto) ─────────────────

    @Data
    public static class EcosystemAccessScopeDto {
        private boolean canViewIcfScore       = false;
        private boolean canViewRiskLevel      = false;
        private boolean canViewPlanSummary    = false;
        private boolean canViewSprintProgress = false;
        private boolean canViewCrisisHistory  = false;
        private boolean canReceiveAlerts      = false;
    }

    // ── Respuestas ────────────────────────────────────────────────────────

    @Data @Builder
    public static class ParticipantResponse {
        private Long id;
        private String name;
        private NetworkType networkType;
        private String description;
        private String contactEmail;
        private String contactPhone;
        private String website;
        private boolean active;
    }

    @Data @Builder
    public static class LinkResponse {
        private Long id;
        private Long familyId;
        private ParticipantResponse participant;
        private NetworkType networkType;
        private int accessLevel;
        private String objective;
        private String responsibilities;
        private LocalDate validFrom;
        private LocalDate validUntil;
        private boolean expired;
        private EcosystemLinkStatus status;
        private String invitedByEmail;
        private LocalDateTime invitedAt;
        private String consentedByEmail;
        private LocalDateTime consentedAt;
        private EcosystemAccessScopeDto accessScope;
    }

    @Data @Builder
    public static class FamilyEcosystemSummary {
        private Long familyId;
        private int totalLinks;
        private int activeLinks;
        private List<LinkResponse> familiar;
        private List<LinkResponse> institutional;
        private List<LinkResponse> community;
        private List<LinkResponse> territorial;
    }
}
