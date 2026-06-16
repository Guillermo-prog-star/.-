package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity
@Table(name = "family_documentaries")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDocumentary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private PlanTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private FamilySprint sprint;

    // Pillar is omitted for now, or we can link it later if required.
    // If needed: private Long pillarId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private DocumentarySourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentaryStatus status = DocumentaryStatus.COMPLETED;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
