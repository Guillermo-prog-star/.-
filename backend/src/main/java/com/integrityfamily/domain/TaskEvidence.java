package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD SPEC 6.4: Entidad de Persistencia de Evidencia Multimodal para Tareas del Plan.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "task_evidences")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaskEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "steps", "plan"})
    private PlanTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "members", "createdBy"})
    private Family family;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EvidenceStatus status = EvidenceStatus.PENDING;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "text_content", columnDefinition = "LONGTEXT")
    private String textContent;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "ai_score")
    private Double aiScore;

    @Column(name = "human_score")
    private Double humanScore;

    @Builder.Default
    private boolean validated = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    /**
     * Método de dominio clínico para validar la evidencia y certificar el cambio conductual.
     */
    public void validate(Double score, String validatorName) {
        this.validated = true;
        this.status = EvidenceStatus.VALIDATED;
        this.humanScore = score;
        this.validatedAt = LocalDateTime.now();
        // Si hay una reflexión clínica o notas de revisión, se pueden concatenar o registrar
    }

    // Métodos explícitos para asegurar la compatibilidad con el compilador en entornos Docker
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    public PlanTask getTask() { return this.task; }
    public void setTask(PlanTask task) { this.task = task; }

    public Family getFamily() { return this.family; }
    public void setFamily(Family family) { this.family = family; }

    public EvidenceType getEvidenceType() { return this.evidenceType; }
    public void setEvidenceType(EvidenceType evidenceType) { this.evidenceType = evidenceType; }

    public EvidenceStatus getStatus() { return this.status; }
    public void setStatus(EvidenceStatus status) { this.status = status; }

    public String getTitle() { return this.title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileUrl() { return this.fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getTextContent() { return this.textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getSubmittedBy() { return this.submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public Double getAiScore() { return this.aiScore; }
    public void setAiScore(Double aiScore) { this.aiScore = aiScore; }

    public Double getHumanScore() { return this.humanScore; }
    public void setHumanScore(Double humanScore) { this.humanScore = humanScore; }

    public boolean isValidated() { return this.validated; }
    public boolean getValidated() { return this.validated; }
    public void setValidated(boolean validated) { this.validated = validated; }

    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getValidatedAt() { return this.validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}
