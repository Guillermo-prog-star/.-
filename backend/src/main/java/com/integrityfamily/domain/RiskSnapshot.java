package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "risk_snapshots")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "score")
    private Double icf;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "has_crisis")
    private Boolean hasCrisis;

    @Column(name = "consciousness_level")
    private Integer consciousnessLevel;

    @Column(name = "consciousness_label")
    private String consciousnessLabel;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * SDD-FIX: Alias para sincronizar con StatusCommandHandler.
     */
    public String getLevel() {
        return this.riskLevel;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }
}
