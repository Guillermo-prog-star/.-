package com.integrityfamily.support.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Borrador de nota de seguimiento profesional generado en backend (ADR-006).
 *
 * sourceSnapshot congela los valores de FamilyDataView vistos al generar
 * -- vienen de estado operacional mutable, y sin congelarlos el mismo
 * borrador "cambiaria" al leerse despues. narrativeText es el texto ya
 * ensamblado con templateVersion, para que el borrador siga siendo
 * interpretable aunque la plantilla evolucione mas adelante.
 *
 * Ciclo de vida minimo (ADR-006, Decision 3): GENERATED al crear,
 * VOIDED automaticamente cuando se genera un borrador mas nuevo para el
 * mismo assignmentId. Nunca se borra.
 */
@Entity
@Table(name = "professional_follow_up_drafts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProfessionalFollowUpDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "generated_by_user_email", nullable = false)
    private String generatedByUserEmail;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "generator_type", nullable = false)
    private DraftGeneratorType generatorType = DraftGeneratorType.RULE_BASED_TEMPLATE;

    @Column(name = "template_version", nullable = false)
    private String templateVersion;

    /** JSON con los valores crudos de FamilyDataView usados al generar. */
    @Column(name = "source_snapshot", columnDefinition = "TEXT", nullable = false)
    private String sourceSnapshot;

    @Column(name = "narrative_text", columnDefinition = "TEXT", nullable = false)
    private String narrativeText;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private DraftStatus status = DraftStatus.GENERATED;

    @PrePersist
    void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
