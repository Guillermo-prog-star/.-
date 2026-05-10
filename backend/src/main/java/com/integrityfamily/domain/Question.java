package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * SDD SPEC: Entidad de Pregunta (Question) centralizada.
 * Extendida para soportar la taxonomía del Modelo Híbrido Adaptativo Longitudinal.
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

    @Builder.Default
    private boolean active = true;

    private int vertice;

    @Builder.Default
    private int weight = 1; // Legado

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
    private String adaptiveTriggers; // disparadores adaptativos, ej: "gritos, evasión"

    @Column(length = 50)
    private String evidenceType; // conductual, fotografica, bitacora, etc.
}
