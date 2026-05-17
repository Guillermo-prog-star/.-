package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD SPEC 6.3: Plan de Mejora Unificado (Plan Híbrido).
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "plans")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ImprovementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "vision_3y", columnDefinition = "TEXT")
    private String vision3y;

    @Column(name = "ai_report", columnDefinition = "TEXT")
    private String aiReport;

    @Column(name = "ai_generated_at")
    private LocalDateTime aiGeneratedAt;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanTask> tasks = new ArrayList<>();

    public List<PlanTask> getTasks() {
        if (this.tasks == null) {
            this.tasks = new ArrayList<>();
        }
        return this.tasks;
    }

    // SDD-FIX: Métodos explícitos para asegurar compatibilidad
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
}
