package com.integrityfamily.guardian.domain;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guardian_votes",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_one_vote_per_member",
           columnNames = {"family_id", "voter_member_id"}
       ))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GuardianVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_member_id", nullable = false)
    private FamilyMember voter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nominated_member_id", nullable = false)
    private FamilyMember nominated;

    @Column(name = "voted_at", updatable = false)
    private LocalDateTime votedAt;

    @PrePersist
    public void prePersist() {
        if (votedAt == null) votedAt = LocalDateTime.now();
    }
}
