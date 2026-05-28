package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_identity_profiles", indexes = {
    @Index(name = "idx_mip_member", columnList = "member_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberIdentityProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    /** DIRECT | REFLECTIVE | AVOIDANT | ASSERTIVE — inferido de conversaciones */
    @Column(name = "communication_style", length = 20)
    private String communicationStyle;

    /** 1=impulsivo … 5=muy reflexivo */
    @Column(name = "reflexivity_level")
    @Builder.Default
    private Integer reflexivityLevel = 3;

    /** 1=muy contenido … 5=muy sensible */
    @Column(name = "emotional_sensitivity")
    @Builder.Default
    private Integer emotionalSensitivity = 3;

    /** JSON array de strings — temas o dinámicas que el miembro evita */
    @Column(name = "evasion_patterns", columnDefinition = "TEXT")
    private String evasionPatterns;

    /** JSON array de strings — qué activa la participación del miembro */
    @Column(name = "motivators", columnDefinition = "TEXT")
    private String motivators;

    /** LOW | MED | HIGH */
    @Column(name = "change_resistance", length = 10)
    @Builder.Default
    private String changeResistance = "MED";

    @Column(name = "last_updated", nullable = false)
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
