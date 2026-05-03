package com.integrityfamily.lts.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lts_errors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private Attempt attempt;

    @Column(name = "error_type", nullable = false)
    private String errorType; // CONCEPTUAL, LOGICAL, PROCEDURAL, ATTENTION

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @OneToOne(mappedBy = "error", cascade = CascadeType.ALL)
    private Hypothesis hypothesis;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
