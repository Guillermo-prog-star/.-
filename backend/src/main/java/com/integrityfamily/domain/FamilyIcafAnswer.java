package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Respuesta de una familia a un ítem del cuestionario ICaF.
 *
 * Una fila por (family_id, question_key): UPSERT en cada nueva respuesta.
 * Se usa para calcular el score de los dominios confianza y bienestar_emocional
 * en IcafDomainResolver, reemplazando las estimaciones de S2.
 */
@Entity
@Table(
    name = "family_icaf_answers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"family_id", "question_key"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyIcafAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Column(name = "icaf_domain", nullable = false, length = 30)
    private String icafDomain;

    /** Respuesta en escala 1-5 */
    @Column(name = "score", nullable = false)
    private Integer score;

    /** Email del miembro respondiente; null = respuesta familiar conjunta */
    @Column(name = "answered_by", length = 120)
    private String answeredBy;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt   = LocalDateTime.now();
        answeredAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        answeredAt = LocalDateTime.now();
    }
}
