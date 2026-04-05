package com.integrityfamily.checklist.domain;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.plan.domain.PlanTask;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad ChecklistItem: Representa un ítem de control para la familia.
 * Corregida para incluir 'createdAt', permitiendo el ordenamiento requerido por el Repositorio.
 */
@Entity
@Table(name = "checklist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_task_id")
    private PlanTask planTask;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "completed")
    private Boolean completed;

    // PROPIEDAD CRÍTICA: Añadida para resolver el error de QueryCreationException
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Lógica Automática: Asegura valores por defecto y marca de tiempo antes de persistir.
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