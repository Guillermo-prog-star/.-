package com.integrityfamily.plan.domain;

import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.family.domain.Family;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Plan: Define la hoja de ruta de bienestar familiar.
 * Se ha añadido 'createdAt' para dar soporte al ordenamiento requerido por PlanRepository.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id")
    private Evaluation evaluation;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    // CAMPO REQUERIDO POR EL REPOSITORIO (Soluciona el error de arranque)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<PlanTask> tasks = new ArrayList<>();

    /**
     * Establece automáticamente la fecha de creación antes de insertar en MySQL.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}