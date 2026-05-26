package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD SPEC 6.2: Tarea de Plan Harmonizada.
 */
@Entity
@Table(name = "plan_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    private ImprovementPlan plan;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String dimension;

    private LocalDateTime dueDate;
    
    private int periodicityMonths;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private FamilyMember responsible;

    @Builder.Default
    private boolean completed = false;

    @Builder.Default
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanTaskStep> steps = new ArrayList<>();

    public List<PlanTaskStep> getSteps() {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        return this.steps;
    }

    private String fase; // RECONOCIMIENTO, AMOR, ENTREGA

    @Column(name = "riesgo_asociado")
    private String riesgoAsociado;

    @Column(columnDefinition = "TEXT")
    private String objetivo;

    @Column(name = "accion_concreta", columnDefinition = "TEXT")
    private String accionConcreta;

    @Column(name = "indicador_cumplimiento", columnDefinition = "TEXT")
    private String indicadorCumplimiento;

    @Column(name = "evidencia_requerida", columnDefinition = "TEXT")
    private String evidenciaRequerida;

    @Column(name = "impacto_icf")
    private Integer impactoIcf;

    // --- Taxonomía Longitudinal v2 ---
    @Column(name = "pillar_name", length = 50)
    private String pillarName; // reconocimiento / amor / entrega

    @Column(name = "milestone_code", length = 50)
    private String milestoneCode; // W1 / M1 / M3 / M6 / M9 / M12 / M18 / M24 / M30 / M36

    @Column(name = "member_type", length = 50)
    private String memberType; // familia / padre / madre / hijo / hija

    @Column(name = "risk_type", length = 100)
    private String riskType; // desconexion_emocional, conflicto_reactivo, etc.

    @Column(name = "mission_generator", length = 100)
    private String missionGenerator; // ESTABILIZACION_EMOCIONAL, LEGADO_CONSCIENTE, etc.

    @com.fasterxml.jackson.annotation.JsonProperty("planId")
    public void setPlanId(Long planId) {
        if (planId != null) {
            this.plan = ImprovementPlan.builder().id(planId).build();
        }
    }

    // SDD-FIX: Métodos explícitos para el build de Docker
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isCompleted() { return this.completed; }
    public boolean getCompleted() { return this.completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getDueDate() { return this.dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}
