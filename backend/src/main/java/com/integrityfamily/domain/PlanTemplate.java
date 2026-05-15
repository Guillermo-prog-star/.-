package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD Sprint 3: Entidad de Plantilla Parametrizada de Plan de Mejora.
 */
@Entity
@Table(name = "plan_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTemplate {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String code; // COMM-L1, EMO-REC, etc.

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String dimension; // emociones, comunicacion, habitos, tiempos

    @Column(length = 30)
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL
}
