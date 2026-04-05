package com.integrityfamily.risk.domain;

import com.integrityfamily.family.domain.Family;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_snapshots")
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
    @JoinColumn(name = "family_id")
    private Family family;

    @Column(name = "risk_level", length = 30)
    private String riskLevel;

    @Column(name = "global_score")
    private Double globalScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (riskLevel == null || riskLevel.isBlank()) {
            riskLevel = "MEDIO";
        }
        if (globalScore == null) {
            globalScore = 0.0;
        }
    }
}