package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "checklist_items")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Family family;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean completed = false;

    private String completedBy;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Column(length = 50)
    private String category;

    @Column(length = 50)
    private String source;

    @Column(length = 50)
    private String dimension;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
