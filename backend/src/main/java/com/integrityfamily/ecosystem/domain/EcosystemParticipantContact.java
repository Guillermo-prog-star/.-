package com.integrityfamily.ecosystem.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ecosystem_participant_contacts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EcosystemParticipantContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private EcosystemParticipant participant;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 180)
    private String email;

    @Column(name = "role_title", length = 120)
    private String roleTitle;

    @Column(length = 30)
    private String phone;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
