package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD SPEC 6.1: Modelo de Hitos Temporales.
 * Define la progresión cronológica de la transformación familiar.
 */
@Entity
@Table(name = "milestone_definitions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code; // W1, M1, M3, M6, M9, M12, M15, M18, M24, M30, M36

    @Column(nullable = false)
    private String label;

    private String title; // Legacy support

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(columnDefinition = "TEXT")
    private String description;

    // SDD-FIX: Métodos explícitos para asegurar compatibilidad en el build
    public String getTitle() {
        return this.title != null ? this.title : this.label;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
