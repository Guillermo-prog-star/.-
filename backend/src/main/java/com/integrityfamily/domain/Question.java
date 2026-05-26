package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD SPEC: Entidad de Pregunta (Question) centralizada.
 * Extendida con taxonomía psicométrica formal para el algoritmo RISK_ALGO_V1.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_key", unique = true)
    private String questionKey; // Q-EMO-REAC-023, REC_EMO_001, etc.

    @Column(nullable = false, length = 500)
    private String text;

    @Column(length = 50)
    private String dimension; // emociones, comunicacion, habitos, tiempos

    @Column(length = 50)
    private String area; // EMOCIONES, COMUNICACION, etc. (Legado)

    @Column(length = 20)
    @Builder.Default
    private String direction = "POSITIVE"; // POSITIVE o NEGATIVE

    @Column(length = 20)
    @Builder.Default
    private String version = "1.0"; // Versión del reactivo

    @Builder.Default
    private boolean active = true;

    private int vertice;

    @Builder.Default
    private int weight = 1;

    private Integer sortOrder;

    // --- Nueva Taxonomía del Modelo Híbrido Adaptativo ---
    
    @Column(length = 50)
    private String pillar; // M00, M03, M06, M12, etc.

    @Column(length = 50)
    private String phase; // reactividad, consciencia, primeros_cambios, etc.

    @Column(length = 50)
    private String type; // CORE, ADAPTIVE, FASE_PILLAR, MIRROR, EXPLORATORY

    private Double severityWeight; // Ponderación de severidad clínica

    private boolean detectsRelapse; // Detecta recaída

    private boolean requiresEvidence; // Requiere adjuntar evidencia física/conductual

    private boolean reverseQuestion; // Pregunta espejo / invertida para detectar simulación

    @Column(length = 100)
    private String category; // escucha, regulacion emocional, orden, etc.

    @Column(name = "adaptive_triggers", length = 255)
    private String adaptiveTriggers;

    @Column(length = 50)
    private String evidenceType; // conductual, fotografica, bitacora

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
}
