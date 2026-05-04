package com.integrityfamily.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SDD: Read Model Desnormalizado para el Dashboard.
 * Optimizado para lecturas ultrarrápidas y visualización en tiempo real.
 */
@Entity
@Table(name = "family_dashboard_views")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FamilyDashboardView {

    @Id
    private Long familyId; // Mismo ID que la familia para acceso directo

    private String familyName;
    private String familyCode;
    
    private BigDecimal latestIcf;
    private String riskLevel;
    
    private Long totalMembers;
    private Long tasksCompleted;
    private Long tasksTotal;
    private Double adherencePercentage;
    
    private Long openCrisesCount;
    private Long learningEntriesCount;
    
    @Column(columnDefinition = "TEXT")
    private String latestAiRecommendation;
    
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
