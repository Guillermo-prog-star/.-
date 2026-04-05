package com.integrityfamily.plan.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad PlanTask: Define una acción específica dentro del plan de bienestar.
 * Se añade 'createdAt' para habilitar el ordenamiento cronológico en los repositorios.
 */
@Entity
@Table(name = "plan_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "completed")
    private Boolean completed;

    // CAMPO DE AUDITORÍA: Vital para evitar el PropertyReferenceException
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Lógica Automática: Asegura el estado inicial de la tarea y su marca de tiempo.
     */
    @PrePersist
    public void prePersist() {
        if (completed == null) {
            completed = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}