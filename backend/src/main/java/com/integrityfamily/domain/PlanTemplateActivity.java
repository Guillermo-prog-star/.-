package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD Sprint 3: Actividad parametrizada dentro de un PlanTemplate.
 */
@Entity
@Table(name = "plan_template_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTemplateActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    @Builder.Default
    private String frequency = "DAILY"; // DAILY, WEEKLY, 3_PER_WEEK

    @Column(name = "duration_days")
    @Builder.Default
    private int durationDays = 7;

    @Column(length = 50)
    @Builder.Default
    private String phase = "1 semana"; // 1 semana, 1 mes, 3 meses, 6 meses
}
