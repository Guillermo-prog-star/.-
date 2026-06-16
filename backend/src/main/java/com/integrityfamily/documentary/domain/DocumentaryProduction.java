package com.integrityfamily.documentary.domain;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.TaskEvidence;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentary_productions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentaryProduction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentaryScope scope;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductionStatus status;

    @ManyToMany
    @JoinTable(
        name = "documentary_production_evidences",
        joinColumns = @JoinColumn(name = "production_id"),
        inverseJoinColumns = @JoinColumn(name = "evidence_id")
    )
    private List<TaskEvidence> curatedEvidences = new ArrayList<>();

    // Narrativa IA o Guion estructurado
    @Column(name = "script_data", columnDefinition = "TEXT")
    private String scriptData; 

    @Column(name = "export_url")
    private String exportUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ProductionStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
