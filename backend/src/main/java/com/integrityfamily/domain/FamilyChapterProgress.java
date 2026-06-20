package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_chapter_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"family_id", "chapter_number"}),
        indexes = @Index(name = "idx_fcp_family", columnList = "family_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyChapterProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Long startedAt = System.currentTimeMillis();
}
