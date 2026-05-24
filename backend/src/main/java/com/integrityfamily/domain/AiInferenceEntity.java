package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

/**
 * SDD Sprint 6: Entidad de Persistencia de Inferencia Estructurada y Gobernanza IA.
 * Registra inmutablemente el prompt, contexto resumido y salida JSON del Copiloto.
 */
@Entity
@Table(name = "ai_inferences")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "context_hash", length = 100)
    private String contextHash;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "inference_result", columnDefinition = "TEXT")
    private String inferenceResult; // JSON de salida de Claude

    @Column(length = 50)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "prompt_used", columnDefinition = "TEXT")
    private String promptUsed;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
