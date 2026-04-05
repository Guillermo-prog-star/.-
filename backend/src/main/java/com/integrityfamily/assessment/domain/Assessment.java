package com.integrityfamily.assessment.domain;

import com.integrityfamily.auth.domain.User;
import com.integrityfamily.family.domain.Family; // Importación necesaria
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family; // Agregado para cumplir con AssessmentService

    private Double emotionalScore;
    private Double financialScore;
    private Double globalScore;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL)
    private List<AssessmentDetail> details;

    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}