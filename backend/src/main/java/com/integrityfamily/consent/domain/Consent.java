package com.integrityfamily.consent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Consentimiento formal de una familia para compartir información con un
 * tercero: un participante del ecosistema, un miembro de la red de apoyo, o
 * — el caso que motiva este módulo — una institución externa del ecosistema
 * de salud (Ministerio, IPS). A diferencia de {@code FamilyEcosystemLink} y
 * {@code FamilySupportAssignment}, donde el consentimiento es un conjunto de
 * columnas dentro de la relación con un tipo de participante específico,
 * aquí el consentimiento es la entidad principal y no asume quién es el
 * receptor — {@code granteeReference} es texto libre para poder apuntar a
 * una institución que no existe como entidad propia en el sistema.
 */
@Entity
@Table(name = "consents")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /** Persona específica dentro de la familia; null = consentimiento a nivel de familia completa. */
    @Column(name = "member_id")
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentPurpose purpose;

    /** Qué se comparte, en texto libre (ej. "ICF_SCORE,RISK_LEVEL,EVALUATIONS"). La codificación formal con SNOMED/LOINC es responsabilidad del Terminology Service (fase posterior). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String scope;

    /** A quién se le comparte: nombre de institución, o referencia libre a un vínculo de ecosystem/support para trazabilidad cruzada. */
    @Column(name = "grantee_reference", nullable = false, length = 255)
    private String granteeReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentStatus status;

    @Column(name = "granted_by_email", nullable = false, length = 180)
    private String grantedByEmail;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_by_email", length = 180)
    private String revokedByEmail;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isActive() {
        if (status != ConsentStatus.GRANTED) return false;
        return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
    }
}
