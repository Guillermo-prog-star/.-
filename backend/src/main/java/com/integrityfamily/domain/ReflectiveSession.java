package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD: Entidad que registra una sesión de reflexión emocional ante un estímulo.
 */
@Entity
@Table(name = "reflective_sessions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ReflectiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private FamilyMember member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stimulus_id", nullable = false)
    private EmotionalStimulus stimulus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reflection;

    @Column(name = "emotional_score", nullable = false)
    private Integer emotionalScore; // Auto-evaluación del usuario (1 a 5)

    @Column(name = "inference_result", columnDefinition = "TEXT")
    private String inferenceResult; // JSON string con el veredicto psicométrico de Claude

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
