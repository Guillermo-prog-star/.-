package com.integrityfamily.lts.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lts_comparisons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LearningSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "v_old_id", nullable = false)
    private Attempt oldVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "v_new_id", nullable = false)
    private Attempt newVersion;

    @Column(name = "diff_analysis", nullable = false, columnDefinition = "TEXT")
    private String diffAnalysis;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
