package com.integrityfamily.risk.domain;

import com.integrityfamily.family.domain.Family;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_snapshots")
public class RiskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "icf")
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

    public RiskSnapshot() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Manual Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }
    public Double getIcf() { return icf; }
    public void setIcf(Double icf) { this.icf = icf; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public Boolean getHasCrisis() { return hasCrisis; }
    public void setHasCrisis(Boolean hasCrisis) { this.hasCrisis = hasCrisis; }
    public Integer getConsciousnessLevel() { return consciousnessLevel; }
    public void setConsciousnessLevel(Integer level) { this.consciousnessLevel = level; }
    public String getConsciousnessLabel() { return consciousnessLabel; }
    public void setConsciousnessLabel(String label) { this.consciousnessLabel = label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}