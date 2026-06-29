package com.integrityfamily.ecosystem.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ecosystem_access_log")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EcosystemAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "access_level")
    private Integer accessLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
