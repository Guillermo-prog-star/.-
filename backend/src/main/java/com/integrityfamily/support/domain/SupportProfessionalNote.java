package com.integrityfamily.support.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_professional_notes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportProfessionalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "support_member_id", nullable = false)
    private Long supportMemberId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_visible_to_family")
    private boolean visibleToFamily = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
