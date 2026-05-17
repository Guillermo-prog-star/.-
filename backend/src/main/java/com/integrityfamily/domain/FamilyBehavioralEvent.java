package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD: Entidad para el Registro Cronológico de Eventos Conductuales (Fricciones y Reparaciones).
 * Soporta la medición del Índice de Velocidad de Reparación Familiar (IVR).
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "family_behavioral_events")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FamilyBehavioralEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer severity; // Rango 1 a 5

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "repaired_at")
    private LocalDateTime repairedAt;

    @Column(name = "repair_description", length = 1000)
    private String repairDescription;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FamilyBehavioralEvent(Family family, String description, Integer severity, LocalDateTime occurredAt) {
        this.family = family;
        this.description = description;
        this.severity = severity;
        this.occurredAt = occurredAt;
        this.createdAt = LocalDateTime.now();
    }
}
