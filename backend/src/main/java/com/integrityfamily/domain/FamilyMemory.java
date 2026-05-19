package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD Fase 1 — Arquitectura Cognitiva Familiar.
 * Capa unificada de memoria multicapa: episódica, semántica, procedural e identitaria.
 * Convierte eventos crudos en conocimiento acumulativo persistente.
 */
@Entity
@Table(name = "family_memory", indexes = {
    @Index(name = "idx_family_memory_family_type", columnList = "family_id, memory_type"),
    @Index(name = "idx_family_memory_importance", columnList = "family_id, importance_score DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    /**
     * EPISODIC  — evento concreto (discusión, hito, crisis, logro)
     * SEMANTIC  — patrón consolidado (ej: "baja comunicación → microacciones efectivas")
     * PROCEDURAL — habilidad operacional reutilizable
     * IDENTITY  — rasgo persistente de la identidad familiar
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 20)
    private MemoryType memoryType;

    /** Clave semántica para búsqueda y agrupación (ej: "communication-pattern", "conflict-style") */
    @Column(name = "semantic_key", length = 100)
    private String semanticKey;

    /** Contenido en JSON o texto estructurado */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Relevancia de esta memoria para el sistema cognitivo.
     * 0.0 = irrelevante / 1.0 = crítica.
     * Usado para compresión cognitiva y priorización de contexto IA.
     */
    @Column(name = "importance_score")
    @Builder.Default
    private Double importanceScore = 0.5;

    /** Origen que generó esta memoria */
    @Column(name = "source_type", length = 50)
    private String sourceType; // EVALUATION, REFLECTION, LEARNING_ENTRY, AI_INFERENCE, MANUAL

    @Column(name = "source_id")
    private Long sourceId;

    /** Fecha de expiración semántica — memorias antiguas de baja importancia se comprimen */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum MemoryType {
        EPISODIC, SEMANTIC, PROCEDURAL, IDENTITY
    }
}
