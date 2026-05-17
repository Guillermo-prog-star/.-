package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "beta_feedback")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    private FamilyMember reporter;

    private int score; // 1-5 stars

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String type; // BUG, SUGGESTION, EXPERIENCE

    private String milestoneAtMoment;
    
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
